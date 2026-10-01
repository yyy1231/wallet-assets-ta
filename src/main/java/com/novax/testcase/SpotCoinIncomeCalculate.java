package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.*;
import com.novax.util.MongoUtils;
import com.novax.util.NewestPriceUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 现货主板区账户-单币种-盈亏分析
 */
public class SpotCoinIncomeCalculate extends BaseTest {

    /**
     * 计算现货主板单币种今日盈亏/盈亏率及累计收益/收益率。
     * <pre>
     *   今日盈亏 = 当前资产 - 零点资产 - 净充提 - 净划转 - 净买卖
     *   盈亏率   = 今日盈亏 / (零点资产 + 充值 + 转入 + 买入)
     *   累计盈亏 = (最新价 - 均价) × 持仓量
     * </pre>
     *
     * @param userId   用户ID
     * @param currency 目标币种（小写，如 "btc"）
     * @param env      目标环境（test/dev）
     * @return {@code [todayEarnings, todayEarningsRatio, cumulativeEarnings, cumulativeRatio]}
     */
    public static BigDecimal[] getCoinPnL(long userId, String currency, String env) {
        Map<String, CoinAccount> spotAllCoin = getSpotAllCoin(userId, env);
        CoinAccount account = spotAllCoin.get(currency);
        if (account == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }

        String todayStr = today.format(DATE_FMT);
        BigDecimal todayEarnings = BigDecimal.ZERO;
        BigDecimal todayEarningsRatio = BigDecimal.ZERO;

        BigDecimal newCurrencyPrice = NewestPriceUtil.newPriceOfSymbol(currency + "_usdt", env);

        // MongoDB user_currency_avg 均价
        BigDecimal dbAvg = BigDecimal.ZERO;
        try {
            MongoCollection<Document> avgCol = MongoUtils.getMongoConnection("superex", "user_currency_avg", env);
            Document avgDoc = avgCol.find(
                    new BasicDBObject("user_id", userId).append("currency", currency)).first();
            if (avgDoc != null) {
                dbAvg = UtilityTools.toBigDecimal(avgDoc.get("avg_usdt"));
            }
        } catch (Exception e) {
            logger.warn("user_currency_avg 查询失败 currency=" + currency + ": " + e.getMessage());
        }

        // MongoDB user_assets_detail：今日 0 点各类型资产（type IN [1,7,8]）
        BigDecimal zeroAsset = BigDecimal.ZERO;
        MongoCollection<Document> detailCol = MongoUtils.getMongoConnection("superex", "user_assets_detail", env);
        BasicDBObject detailFilter = new BasicDBObject("user_id", userId)
                .append("currency", currency)
                .append("type", new BasicDBObject("$in", Arrays.asList(1, 7, 8)))
                .append("create_time", UtilityTools.toDate(todayUtc));
        List<Document> zeroAssets = UtilityTools.queryMongo(detailCol, detailFilter, null);
        for (Document doc : zeroAssets) {
            zeroAsset = zeroAsset.add(UtilityTools.toBigDecimal(doc.get("amount_usdt")));
            // 若今日零点单币资产为0，则当前资产皆是今日充值购买或转入，不计为盈亏，即今日盈亏为0，今日收益率亦为0
            if (zeroAsset.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal charge = getChargeByCurrency(userId, currency, todayStr, env);
                BigDecimal withdraw = getWithdrawByCurrency(userId, currency, todayStr, env);
                BigDecimal transferIn = getSpotTransferInByCurrency(userId, currency, todayStr);
                BigDecimal transferOut = getSpotTransferOutByCurrency(userId, currency, todayStr);
                BigDecimal[] buySell = getSpotTrade(userId, currency, todayStr);
                BigDecimal buy = buySell[0];
                BigDecimal sell = buySell[1];

                // 今日盈亏 = 当前资产 - 零点资产 - 充值 + 提现 - 转入 + 转出 - 买入 + 卖出
                BigDecimal amountUsdt = account.usdt;
                todayEarnings = amountUsdt
                        .subtract(zeroAsset)
                        .subtract(charge).add(withdraw)
                        .subtract(transferIn).add(transferOut)
                        .subtract(buy).add(sell);

                try {
                    BigDecimal divisor = zeroAsset.add(charge).add(transferIn).add(buy);
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("除数为零");
                    todayEarningsRatio = UtilityTools.round(
                            todayEarnings.divide(divisor, 20, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100")), 2);
                } catch (ArithmeticException e) {
                    todayEarningsRatio = BigDecimal.ZERO;
                }
            }
        }

        // 累计盈亏 = (最新价 - 均价) × 持仓量
        BigDecimal cumulativeEarnings = newCurrencyPrice.subtract(dbAvg).multiply(account.amount);
        BigDecimal cumulativeRatio;
        try {
            if (dbAvg.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("均价为零");
            cumulativeRatio = UtilityTools.round(
                    newCurrencyPrice.subtract(dbAvg).divide(dbAvg, 20, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")), 2);
        } catch (ArithmeticException e) {
            cumulativeEarnings = BigDecimal.ZERO;
            cumulativeRatio = BigDecimal.ZERO;
        }

        logger.info("currency=" + currency + " 今日盈亏：" + todayEarnings + " 今日盈亏率：" + todayEarningsRatio
                + " 累计收益：" + cumulativeEarnings + " 累计收益率：" + cumulativeRatio);
        return new BigDecimal[]{todayEarnings, todayEarningsRatio, cumulativeEarnings, cumulativeRatio};
    }

    /**
     * 合并三个资产列表：spot + ET奖励 + SCS奖励。
     *
     * @param userId 用户ID
     * @return 以 currency 为 key、{@link CoinAccount} 为 value 的合并资产 Map
     */
    public static Map<String, CoinAccount> getSpotAllCoin(long userId, String env) {
        Map<String, CoinAccount> spotList = buildSpotCoinList(UserAssetsManage.querySpotAssets(userId), env);
        Map<String, CoinAccount> etList = buildEtCoinList(UserAssetsManage.queryEtRewards(userId), env);
        Map<String, CoinAccount> scsList = buildScsCoinList(UserAssetsManage.queryScsRewards(userId), env);

        // 合并 ET 奖励到 spot（只合并已存在的币种）
        for (Map.Entry<String, CoinAccount> e : etList.entrySet()) {
            String currency = e.getKey();
            if (spotList.containsKey(currency)) {
                spotList.get(currency).add(e.getValue());
            }
        }
        // 合并 SCS 奖励
        for (Map.Entry<String, CoinAccount> e : scsList.entrySet()) {
            String currency = e.getKey();
            if (spotList.containsKey(currency)) {
                spotList.get(currency).add(e.getValue());
            }
        }
        return spotList;
    }

    /**
     * 将 spot_user_assets 转换为 CoinAccount map。
     * total = available + frozen，usdt = total × Redis 现货最新价（spot:new:price）。
     *
     * @param rows spot_user_assets 查询结果
     * @return 以 currency 为 key 的资产 Map
     */
    public static Map<String, CoinAccount> buildSpotCoinList(List<SpotUserAssetsEntity> rows, String env) {
        Map<String, CoinAccount> result = new LinkedHashMap<>();
        for (SpotUserAssetsEntity row : rows) {
            BigDecimal total = row.getAvailable().add(row.getFrozen());
            if (total.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(row.getCurrency() + "_usdt", env);
            CoinAccount acc = result.computeIfAbsent(row.getCurrency(), k -> new CoinAccount());
            acc.amount = acc.amount.add(total);
            acc.usdt = acc.usdt.add(total.multiply(usdtPrice));
        }
        return result;
    }

    /**
     * 将 task_et_rewards 转换为 CoinAccount map。
     * total = totalFreeze - unfreeze。
     *
     * @param rows task_et_rewards 查询结果
     * @return 以 currency 为 key 的资产 Map
     */
    public static Map<String, CoinAccount> buildEtCoinList(List<TaskEtRewardsEntity> rows, String env) {
        Map<String, CoinAccount> result = new LinkedHashMap<>();
        for (TaskEtRewardsEntity row : rows) {
            BigDecimal total = row.getTotalFreeze().subtract(row.getUnfreeze());
            if (total.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(row.getCurrency() + "_usdt", env);
            CoinAccount acc = result.computeIfAbsent(row.getCurrency(), k -> new CoinAccount());
            acc.amount = acc.amount.add(total);
            acc.usdt = acc.usdt.add(total.multiply(usdtPrice));
        }
        return result;
    }

    /**
     * 将 activity_user_rewards 转换为 CoinAccount map。
     * total = totalFreeze - unfreeze。
     *
     * @param rows activity_user_rewards 查询结果
     * @return 以 currency 为 key 的资产 Map
     */
    public static Map<String, CoinAccount> buildScsCoinList(List<ActivityUserRewardsEntity> rows, String env) {
        Map<String, CoinAccount> result = new LinkedHashMap<>();
        for (ActivityUserRewardsEntity row : rows) {
            BigDecimal total = row.getTotalFreeze().subtract(row.getUnfreeze());
            if (total.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(row.getCurrency() + "_usdt", env);
            CoinAccount acc = result.computeIfAbsent(row.getCurrency(), k -> new CoinAccount());
            acc.amount = acc.amount.add(total);
            acc.usdt = acc.usdt.add(total.multiply(usdtPrice));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // 充提/划转/买卖 按单币种汇总
    // -------------------------------------------------------------------------

    /**
     * 汇总指定日期内指定币种充值总额（USDT）。
     * 链上充值 amount×new_price + 内部转入 amount×redis_price。
     *
     * @param userId     用户ID
     * @param currency   目标币种（如 "btc"）
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 该币种充值 USDT 总额
     */
    public static BigDecimal getChargeByCurrency(long userId, String currency, String updateTime, String env) {
        BigDecimal sum = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyChargeOrders = UserAssetsManage.queryUserCurrencyChargeOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyChargeOrders) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(item.getNewPrice()));
            }
        }
        List<InternalTransferOrderEntity> internalTransferOrders = UserAssetsManage.queryInternalTransferOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferOrders) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(currency + "_usdt", env)));
            }
        }
        return sum;
    }

    /**
     * 汇总指定日期内指定币种提现总额（USDT）。
     *
     * @param userId     用户ID
     * @param currency   目标币种
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 该币种提现 USDT 总额
     */
    public static BigDecimal getWithdrawByCurrency(long userId, String currency, String updateTime, String env) {
        BigDecimal sum = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyWithdrawOrders = UserAssetsManage.queryUserCurrencyWithdrawOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyWithdrawOrders) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(item.getNewPrice()));
            }
        }
        List<InternalTransferOrderEntity> internalTransferWithdrawOrders = UserAssetsManage.queryInternalTransferWithdrawOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferWithdrawOrders) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(currency + "_usdt", env)));
            }
        }
        return sum;
    }

    /**
     * 汇总指定日期内指定币种现货账户划转转入（USDT）。
     * type IN (2,4,6,8,10,12,14)。
     *
     * @param userId     用户ID
     * @param currency   目标币种
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 该币种划入 USDT 总额
     */
    public static BigDecimal getSpotTransferInByCurrency(long userId, String currency, String updateTime) {
        BigDecimal sum = BigDecimal.ZERO;
        List<SpotTransferOrderEntity> spotTransferIns = UserAssetsManage.querySpotTransferIn(userId, updateTime);
        for (SpotTransferOrderEntity item : spotTransferIns) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(item.getNewPrice()));
            }
        }
        return sum;
    }

    /**
     * 汇总指定日期内指定币种现货账户划转转出（USDT）。
     * type IN (1,3,5,7,9,11,13)。
     *
     * @param userId     用户ID
     * @param currency   目标币种
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 该币种划出 USDT 总额
     */
    public static BigDecimal getSpotTransferOutByCurrency(long userId, String currency, String updateTime) {
        BigDecimal sum = BigDecimal.ZERO;
        List<SpotTransferOrderEntity> spotTransferOuts = UserAssetsManage.querySpotTransferOut(userId, updateTime);
        for (SpotTransferOrderEntity item : spotTransferOuts) {
            if (currency.equals(item.getCurrency())) {
                sum = sum.add(item.getAmount().multiply(item.getNewPrice()));
            }
        }
        return sum;
    }

    /**
     * 汇总今日指定币种现货买入/卖出金额（USDT）。
     *
     * @param userId   用户ID
     * @param currency 目标币种
     * @param todayStr 日期字符串 yyyy-MM-dd
     * @return {@code BigDecimal[]{buy, sell}}，buy 为买入 USDT 总额，sell 为卖出 USDT 总额
     */
    public static BigDecimal[] getSpotTrade(long userId, String currency, String todayStr) {
        List<SpotOrderRecordEntity> orders = UserAssetsManage.querySpotTodayOrders(userId, todayStr);
        BigDecimal buy = BigDecimal.ZERO;
        BigDecimal sell = BigDecimal.ZERO;
        for (SpotOrderRecordEntity order : orders) {
            String symbol = order.getSymbol();
            if (symbol == null || !symbol.contains(currency)) continue;

            int tradeType = order.getTradeType();
            BigDecimal usdtPrice = order.getUsdtPrice();
            String[] parts = symbol.split("_");
            if (parts.length != 2) continue;
            String left = parts[0];
            String right = parts[1];

            if (tradeType == 1) {
                // 买入
                if (currency.equals(left)) buy = buy.add(usdtPrice);
                else if (currency.equals(right)) sell = sell.add(usdtPrice);
            } else if (tradeType == 2) {
                // 卖出
                if (currency.equals(left)) sell = sell.add(usdtPrice);
                else if (currency.equals(right)) buy = buy.add(usdtPrice);
            }
        }
        return new BigDecimal[]{buy, sell};
    }

    /**
     * 持仓账户信息：amount=持仓量（原始数量），usdt=折算后 USDT 价值。
     */
    public static class CoinAccount {
        public BigDecimal amount = BigDecimal.ZERO;
        public BigDecimal usdt = BigDecimal.ZERO;

        /**
         * 将另一个 CoinAccount 的 amount 和 usdt 累加到当前账户。
         *
         * @param other 被合并的账户
         */
        public void add(CoinAccount other) {
            this.amount = this.amount.add(other.amount);
            this.usdt = this.usdt.add(other.usdt);
        }
    }

    /**
     * 现货币种本地资产快照，用于与 /spot/assets assetList 逐字段断言对比。
     * 包含持仓量、PnL、均价全量字段，由 {@link #getSpotCoinDetailMap} 统一构建。
     */
    public static class SpotCoinDetail {
        // 资产统计
        public BigDecimal balance = BigDecimal.ZERO; // 总持仓量（原始数量）
        public BigDecimal available = BigDecimal.ZERO; // 可用量（含 ET/SCS 净额）
        public BigDecimal frozen = BigDecimal.ZERO; // 冻结量（仅 spot）
        // public BigDecimal valuationCurrencyBtc = BigDecimal.ZERO; // BTC估值

        // 盈亏统计
        public BigDecimal pnlDay = BigDecimal.ZERO; // 今日盈亏（USDT）
        public BigDecimal pnlRatioDay = BigDecimal.ZERO; // 今日盈亏率（%）
        public BigDecimal pnlCumulative = BigDecimal.ZERO; // 持有收益（USDT）
        public BigDecimal pnlCumulativeRatio = BigDecimal.ZERO; // 持有收益率（%）
        public BigDecimal avg = BigDecimal.ZERO; // 成本均价（user_currency_avg）
    }

    /**
     * 按币种构建本地资产全量快照 map，与 /spot/assets assetList 所有可断言字段对应。
     * key = 币种小写，value = {@link SpotCoinDetail}（持仓量 + PnL + 均价）
     * <ul>
     *   <li>balance / available / frozen：spot 账户 + ET/SCS 奖励净额</li>
     *   <li>avg / pnl*：复用 {@link #getCoinPnL} 逐币计算</li>
     * </ul>
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return 币种小写 → SpotCoinDetail
     */
    public static Map<String, SpotCoinDetail> getSpotCoinDetailMap(long userId, String env) {
        Map<String, SpotCoinDetail> map = new HashMap<>();

        // 1. spot 账户：available + frozen
        List<SpotUserAssetsEntity> userAssetsEntityList = UserAssetsManage.querySpotAssets(userId);
        for (SpotUserAssetsEntity a : userAssetsEntityList) {
            String key = a.getCurrency().toLowerCase();
            SpotCoinDetail d = map.computeIfAbsent(key, k -> new SpotCoinDetail());
            d.available = d.available.add(a.getAvailable());
            d.frozen = d.frozen.add(a.getFrozen());
            d.balance = d.balance.add(a.getAvailable()).add(a.getFrozen());
        }
        // 2. ET 奖励净额：计入 available + balance
        List<TaskEtRewardsEntity> taskEtRewardsEntityList = UserAssetsManage.queryEtRewards(userId);
        for (TaskEtRewardsEntity r : taskEtRewardsEntityList) {
            BigDecimal net = r.getTotalFreeze().subtract(r.getUnfreeze());
            if (net.compareTo(BigDecimal.ZERO) <= 0) continue;
            String key = r.getCurrency().toLowerCase();
            SpotCoinDetail d = map.computeIfAbsent(key, k -> new SpotCoinDetail());
            d.available = d.available.add(net);
            d.balance = d.balance.add(net);
        }
        // 3. SCS 奖励净额：计入 available + balance
        List<ActivityUserRewardsEntity> userRewardsEntityList = UserAssetsManage.queryScsRewards(userId);
        for (ActivityUserRewardsEntity r : userRewardsEntityList) {
            BigDecimal net = r.getTotalFreeze().subtract(r.getUnfreeze());
            if (net.compareTo(BigDecimal.ZERO) <= 0) continue;
            String key = r.getCurrency().toLowerCase();
            SpotCoinDetail d = map.computeIfAbsent(key, k -> new SpotCoinDetail());
            d.available = d.available.add(net);
            d.balance = d.balance.add(net);
        }
        // 4. 均价 + PnL：复用 getCoinPnL()，逐币种填入
        for (Map.Entry<String, SpotCoinDetail> e : map.entrySet()) {
            String currency = e.getKey();
            SpotCoinDetail d = e.getValue();
            try {
                // avg：MongoDB user_currency_avg
                MongoCollection<Document> col =
                        MongoUtils.getMongoConnection("superex", "user_currency_avg", env);
                Document doc = col.find(
                        new BasicDBObject("user_id", userId)
                                .append("currency", currency)).first();
                if (doc != null) {
                    d.avg = UtilityTools.toBigDecimal(doc.get("avg_usdt"));
                }
                // PnL：[pnlDay, pnlRatioDay, pnlCumulative, pnlCumulativeRatio]
                BigDecimal[] pnl = getCoinPnL(userId, currency, env);
                d.pnlDay = pnl[0];
                d.pnlRatioDay = pnl[1];
                d.pnlCumulative = pnl[2];
                d.pnlCumulativeRatio = pnl[3];
            } catch (Exception ex) {
                logger.warn("SpotCoinDetail PnL/avg 填充失败 currency=" + currency + ": " + ex.getMessage());
            }
        }
        logger.info("SpotCoinDetailMap size=" + map.size());
        return map;
    }
}
