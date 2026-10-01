package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.*;
import com.novax.util.MongoUtils;
import com.novax.util.NewestPriceUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 钱包总览今日资产10分钟趋势分析：汇总全量账户 USDT/BTC 资产，
 * 与 MongoDB assets_statistics_ten_new 快照对比（仅日志，无断言）。
 */
public class TodayTenMinAssetCalculate extends BaseTest {
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    /**
     * MongoDB assets_statistics_ten_new 最新快照
     * 并与当前计算的 USDT/BTC 资产总额对比（仅日志，无断言）
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdtTotal, btcTotal]}
     */
    public static BigDecimal[] getCurrentTotal(long userId, String env) {
        // MongoDB 最新快照
        MongoCollection<Document> col = MongoUtils.getMongoConnection(
                "superex", "assets_statistics_ten_new", env);
        List<Document> mongoData = new ArrayList<>();
        col.find(new BasicDBObject("user_id", userId))
                .sort(new Document("create_time", -1))
                .limit(1)
                .into(mongoData);
        if (!mongoData.isEmpty()) {
            Document doc = mongoData.get(0);
            BigDecimal totalUsdt = UtilityTools.toBigDecimal(doc.get("total_usdt"));
            BigDecimal totalBtc = UtilityTools.toBigDecimal(doc.get("total_btc"));
            logger.error("以下为MongoDB查询结果  " + doc.get("_id"));
            logger.error("【USDT】   " + totalUsdt);
            logger.error("【BTC】   " + totalBtc);
            return new BigDecimal[]{totalUsdt, totalBtc};
        } else {
            logger.error("没有发现该用户的数据信息");
        }
        return null;
    }

    /**
     * 汇总全量账户 USDT/BTC 资产
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格及 MongoDB 查询
     * @return {@code [usdtTotal, btcTotal]}
     */
    public static BigDecimal[] currentWalletAssetsTotal(long userId, String env) {
        // 现货（Redis 价格）
        BigDecimal[] spot = getSpotAmount(userId, env);
        // 合约（价格）
        BigDecimal[] swap = getSwapAmount(userId, env);
        // 合约未实现盈亏（合约基础+跟单，价格）
        BigDecimal[] swapEarnings = getSwapPositionAmount(userId, env);
        // P2P（价格）
        BigDecimal[] p2p = getP2pAmount(userId, env);
        // 理财-利息（usdt 字段以 BTC 折算填充，与 btc 字段相同）
        BigDecimal[] financeInterest = getFinanceAmount(userId, env);
        // 理财-持仓（价格）
        BigDecimal[] financeDeposit = getFinanceDepositAmount(userId, env);
        // ET 奖励（价格）
        BigDecimal[] etRewards = getEtRewardsAmount(userId, env);
        // SCS 奖励（价格）
        BigDecimal[] scs = getScsAmount(userId, env);
        // 自由区（近似：以现货价格代替 currency_id 专属价格）
        BigDecimal[] free = getFreeSpotAmount(userId, env);
        // 网格（Redis 价格）
        BigDecimal[] grid = getGridAmount(userId, env);
        // 合约跟单（Redis 价格）
        BigDecimal[] swapCopy = getSwapCopyAmount(userId, env);
        // 全币种合约（Redis 价格）
        BigDecimal[] multiSwap = getMultiCurrencySwapAmount(userId, env);
        // 全币种合约未实现盈亏（Redis swap:index:price）
        BigDecimal[] multiSwapPosition = getMultiCurrencySwapPositionAmount(userId, env);

        BigDecimal usdtTotal = spot[0].add(swap[0]).add(swapEarnings[0]).add(p2p[0])
                .add(financeDeposit[0]).add(financeInterest[0]).add(etRewards[0])
                .add(scs[0]).add(free[0]).add(grid[0]).add(swapCopy[0])
                .add(multiSwap[0]).add(multiSwapPosition[0]);
        BigDecimal btcTotal = spot[1].add(swap[1]).add(swapEarnings[1]).add(p2p[1])
                .add(financeDeposit[1]).add(financeInterest[1]).add(etRewards[1])
                .add(scs[1]).add(free[1]).add(grid[1]).add(swapCopy[1])
                .add(multiSwap[1]).add(multiSwapPosition[1]);

        logger.error("【USDT】" + UtilityTools.truncate(usdtTotal, 6));
        logger.error("【BTC】" + UtilityTools.truncate(btcTotal, 6));

        return new BigDecimal[]{usdtTotal, btcTotal};
    }

    /**
     * 现货账户（available + frozen），价格来源 Redis。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getSpotAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<SpotUserAssetsEntity> spotAssets = UserAssetsManage.querySpotAssets(userId);
        for (SpotUserAssetsEntity asset : spotAssets) {
            BigDecimal total = asset.getAvailable().add(asset.getFrozen());
            if (total.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("现货 USDT总额：" + usdt);
        logger.warn("现货 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 合约账户（available only），价格来源 Redis（spot:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getSwapAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<SwapUserAssetsEntity> swapAssets = UserAssetsManage.querySwapAssets(userId);
        for (SwapUserAssetsEntity asset : swapAssets) {
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(asset.getAvailable().multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(asset.getAvailable().multiply(usdtPrice), "usdt", env));
        }
        logger.warn("合约 USDT总额：" + usdt);
        logger.warn("合约 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 合约未实现盈亏（合约基础+跟单，全量仓位），价格来源 Redis（swap:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getSwapPositionAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<SwapFrontPositionEntity> swapPositions = UserAssetsManage.querySwapPositions(userId);
        for (SwapFrontPositionEntity item : swapPositions) {
            BigDecimal newPrice = NewestPriceUtil.newPriceOfSwap(item.getSymbol(), env);
            BigDecimal costOpen = item.getCostOpen();
            int direction = item.getDirection();
            BigDecimal volume = BigDecimal.valueOf(item.getVolume());
            BigDecimal contractSize = item.getContractSize();
            if (costOpen.compareTo(BigDecimal.ZERO) == 0) continue;
            if (direction != 1 && direction != 2) {
                logger.warn("swap_front_position direction 错误：" + direction);
                continue;
            }
            BigDecimal pnl = UtilityTools.calcSwapPnl(direction, newPrice, costOpen, volume, contractSize);
            usdt = usdt.add(pnl);
            btc = btc.add(changeToBtcAmount(pnl, "usdt", env));
        }
        logger.warn("合约未实现盈亏 USDT总额：" + usdt);
        logger.warn("合约未实现盈亏 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * P2P 账户（available + cash_deposit + frozen），价格来源 Redis（spot:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getP2pAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<OtcUserAssetsEntity> otcAssets = UserAssetsManage.queryOtcAssets(userId);
        for (OtcUserAssetsEntity asset : otcAssets) {
            BigDecimal total = asset.getAvailable()
                    .add(asset.getCashDeposit())
                    .add(asset.getFrozen());
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("P2P USDT总额：" + usdt);
        logger.warn("P2P BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 理财-利息账户（balance），价格来源 Redis（spot:new:price）。
     * 注意：usdt/btc 字段均返回 BTC 折算值（usdt 字段非 USDT，历史兼容逻辑）。
     *
     * @param userId 用户ID
     * @return {@code [btc折算值, btc折算值]}（usdt 字段与 btc 字段相同，均为 BTC 折算后的值）
     */
    public static BigDecimal[] getFinanceAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<FinanceAccountEntity> financeAccounts = UserAssetsManage.queryFinanceAccounts(userId);
        for (FinanceAccountEntity asset : financeAccounts) {
            BigDecimal usdtVal = asset.getBalance().multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env));
            BigDecimal btcVal  = changeToBtcAmount(usdtVal, "usdt", env);
            usdt = usdt.add(usdtVal);
            btc  = btc.add(btcVal);
        }
        logger.warn("理财-利息 USDT总额：" + usdt);
        logger.warn("理财-利息 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 理财-持仓（amount，state IN (1,2)），价格来源 Redis（spot:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getFinanceDepositAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<FinanceDepositEntity> activeFinanceDeposits = UserAssetsManage.queryActiveFinanceDeposits(userId);
        for (FinanceDepositEntity asset : activeFinanceDeposits) {
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(asset.getAmount().multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(asset.getAmount().multiply(usdtPrice), "usdt", env));
        }
        logger.warn("理财-持仓 USDT总额：" + usdt);
        logger.warn("理财-持仓 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * ET 奖励（total_freeze - unfreeze），价格来源 Redis（spot:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getEtRewardsAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<TaskEtRewardsEntity> etRewards = UserAssetsManage.queryEtRewards(userId);
        for (TaskEtRewardsEntity reward : etRewards) {
            BigDecimal total = reward.getTotalFreeze().subtract(reward.getUnfreeze());
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(reward.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("ET奖励 USDT总额：" + usdt);
        logger.warn("ET奖励 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * SCS 奖励（total_freeze - unfreeze），价格来源 Redis（spot:new:price）。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getScsAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<ActivityUserRewardsEntity> scsRewards = UserAssetsManage.queryScsRewards(userId);
        for (ActivityUserRewardsEntity reward : scsRewards) {
            BigDecimal total = reward.getTotalFreeze().subtract(reward.getUnfreeze());
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(reward.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("SCS奖励 USDT总额：" + usdt);
        logger.warn("SCS奖励 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 自由区现货账户（available + frozen），价格来源 Redis（spot:new:price，近似）。
     * 注意：以现货价格代替 currency_id 专属价格，存在轻微误差。
     *
     * @param userId 用户ID
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getFreeSpotAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<FreeTradeSpotUserAssetsEntity> freeTradeSpotAssets = UserAssetsManage.queryFreeTradeSpotAssets(userId);
        for (FreeTradeSpotUserAssetsEntity asset : freeTradeSpotAssets) {
            BigDecimal total = asset.getAvailable().add(asset.getFrozen());
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("自由区 USDT总额：" + usdt);
        logger.warn("自由区 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 网格账户（available + frozen），价格来源 Redis。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getGridAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<GridSpotUserAssetsEntity> gridSpotAssets = UserAssetsManage.queryGridSpotAssets(userId);
        for (GridSpotUserAssetsEntity asset : gridSpotAssets) {
            BigDecimal total = asset.getAvailable().add(asset.getFrozen());
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(total.multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(total.multiply(usdtPrice), "usdt", env));
        }
        logger.warn("网格 USDT总额：" + usdt);
        logger.warn("网格 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 合约跟单账户（available only），价格来源 Redis。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getSwapCopyAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<SwapCopyUserAssetsEntity> swapCopyAssets = UserAssetsManage.querySwapCopyAssets(userId);
        for (SwapCopyUserAssetsEntity asset : swapCopyAssets) {
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(asset.getAvailable().multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(asset.getAvailable().multiply(usdtPrice), "usdt", env));
        }
        logger.warn("合约跟单 USDT总额：" + usdt);
        logger.warn("合约跟单 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 全币种合约账户（available only），价格来源 Redis。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getMultiCurrencySwapAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<MultiCurrencySwapUserAssetsEntity> multiCurrencySwapAssets = UserAssetsManage.queryMultiCurrencySwapAssets(userId);
        for (MultiCurrencySwapUserAssetsEntity asset : multiCurrencySwapAssets) {
            BigDecimal usdtPrice = NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env);
            usdt = usdt.add(asset.getAvailable().multiply(usdtPrice));
            btc = btc.add(changeToBtcAmount(asset.getAvailable().multiply(usdtPrice), "usdt", env));
        }
        logger.warn("全币种账户 USDT总额：" + usdt);
        logger.warn("全币种账户 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 全币种合约未实现盈亏，价格来源 Redis swap:index:price。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [usdt总额, btc总额]}
     */
    public static BigDecimal[] getMultiCurrencySwapPositionAmount(long userId, String env) {
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal btc = BigDecimal.ZERO;
        List<MultiCurrencySwapPositionEntity> multiCurrencySwapPositions = UserAssetsManage.queryMultiCurrencySwapPositions(userId);
        for (MultiCurrencySwapPositionEntity item : multiCurrencySwapPositions) {
            String symbol = item.getSymbol().replace(item.getCurrency(), "usdt");
            BigDecimal newPrice = NewestPriceUtil.getSwapIndexPrice(symbol, env);
            BigDecimal costOpen = item.getCostOpen();
            int direction = item.getDirection();
            BigDecimal volume = item.getVolume();
            if (costOpen.compareTo(BigDecimal.ZERO) == 0) continue;
            if (direction != 1 && direction != 2) {
                logger.warn("multi_currency_swap_position direction 错误：" + direction);
                continue;
            }
            BigDecimal profit = UtilityTools.calcMultiSwapPnl(direction, newPrice, costOpen, volume);
            BigDecimal currencyUsdtPrice = NewestPriceUtil.getSwapIndexPrice(item.getCurrency() + "_usdt", env);
            BigDecimal usdtEr = profit.multiply(currencyUsdtPrice);
            usdt = usdt.add(usdtEr);
            btc = btc.add(changeToBtcAmount(usdtEr, "usdt", env));
        }
        logger.warn("全币种合约未实现盈亏 USDT总额：" + usdt);
        logger.warn("全币种合约未实现盈亏 BTC总额：" + btc);
        return new BigDecimal[]{usdt, btc};
    }

    /**
     * 将指定货币金额换算为 BTC，价格来源 Redis（spot:new:price）。
     *
     * @param amount 待换算金额（当 currency='usdt' 时即为 USDT 金额）
     * @param currency   金额对应的货币（如 "usdt"、"btc"）
     * @return 换算后的 BTC 数量，BTC 价格为零时返回 ZERO
     */
    public static BigDecimal changeToBtcAmount(BigDecimal amount, String currency, String env) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal usdtValue;
        if ("usdt".equals(currency)) {
            usdtValue = amount;
        } else {
            usdtValue = amount.multiply(NewestPriceUtil.newPriceOfSymbol(currency + "_usdt", env));
        }
        BigDecimal btcPrice = NewestPriceUtil.newPriceOfSymbol("btc_usdt", env);
        if (btcPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return usdtValue.divide(btcPrice, 20, RoundingMode.HALF_UP);
    }

    public static void main(String[] args) {
        getCurrentTotal(28675872L, "test");
    }
}
