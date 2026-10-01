package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.*;
import com.novax.mapper.SwapFrontPositionEntityMapper;
import com.novax.mapper.SwapFrontUserAssetsEntityMapper;
import com.novax.mapper.SwapFrontUserConfigEntityMapper;
import com.novax.util.MongoUtils;
import com.novax.util.MyBatisUtil;
import com.novax.util.NewestPriceUtil;
import org.bson.Document;
import org.testng.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合约账户盈亏分析：支持今日、近7日、近30日盈亏计算，返回盈亏值供测试类断言。
 */
public class SwapIncomeCalculate extends BaseTest {

    /**
     * 合约账户盈亏计算入口。
     *
     * @param userId 用户ID
     * @param type   校验类型：1=今日，7=近7日，其他=近30日
     * @param env    目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] getCyclicalPnL(long userId, int type, String env) throws Exception {
        if (type == 1) return swapTodayPnL(userId, env);
        else if (type == 7) return swapSevenPnL(userId, env);
        else return swapThirtyPnL(userId, env);
    }

    /**
     * 计算合约账户今日盈亏。
     * 公式：今日盈亏 = 期末 - 期初 - (转入 - 转出) + 未实现盈亏 - 期初浮动
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [今日盈亏USDT, 今日盈亏率]}
     */
    public static BigDecimal[] swapTodayPnL(long userId, String env) {
        String todayStr = today.format(DATE_FMT);

        BigDecimal end = getCurrentSwapAccount(userId, env);
        logger.info("期末资产：" + end);

        BigDecimal start = BigDecimal.ZERO;
        BigDecimal floatBegin = BigDecimal.ZERO;
        MongoCollection<Document> dayCol = MongoUtils.getMongoConnection(
                "superex", "user_assets_statistics_swap_day", env);
        BasicDBObject dayFilter = new BasicDBObject("user_id", userId)
                .append("create_time", UtilityTools.toDate(todayUtc));
        List<Document> dayDocs = UtilityTools.queryMongo(dayCol, dayFilter, null);
        if (dayDocs.size() == 1) {
            start = UtilityTools.toBigDecimal(dayDocs.get(0).get("total_usdt"));
            floatBegin = UtilityTools.toBigDecimal(dayDocs.get(0).get("position_usdt"));
        }
        logger.info("期初资产：" + start + "，期初浮动：" + floatBegin);

        BigDecimal transferIn = getSwapTransferInTotal(userId, todayStr);
        logger.info("转入：" + transferIn);

        BigDecimal transferOut = getSwapTransferOutTotal(userId, todayStr);
        logger.info("转出：" + transferOut);

        BigDecimal position = getSwapFrontPositionAmount(userId, env);
        logger.info("未实现盈亏：" + position);

        BigDecimal todayEarnings = end.subtract(start)
                .subtract(transferIn.subtract(transferOut))
                .add(position)
                .subtract(floatBegin);
        logger.info("今日盈亏：" + todayEarnings);

        BigDecimal divisor = start.add(transferIn).add(floatBegin);
        BigDecimal todayEarningsRatio;
        if (todayEarnings.compareTo(BigDecimal.ZERO) > 0 && divisor.compareTo(BigDecimal.ZERO) == 0) {
            todayEarningsRatio = BigDecimal.ONE.multiply(new BigDecimal("100"));
            return new BigDecimal[]{todayEarnings, todayEarningsRatio};
        }
        if (todayEarnings.compareTo(BigDecimal.ZERO) == 0 || divisor.compareTo(BigDecimal.ZERO) == 0) {
            todayEarningsRatio = BigDecimal.ZERO;
            return new BigDecimal[]{todayEarnings, todayEarningsRatio};
        }
        todayEarningsRatio = todayEarnings
                .divide(divisor, 20, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        logger.info("今日盈亏率：" + todayEarningsRatio);
        return new BigDecimal[]{todayEarnings, todayEarningsRatio};
    }

    /**
     * 计算合约账户近7日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近7日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] swapSevenPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, sevenUtc, 7, env);
    }

    /**
     * 计算合约账户近30日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近30日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] swapThirtyPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, thirtyUtc, 30, env);
    }

    /**
     * 从 MongoDB 查询合约账户周期盈亏。
     *
     * @param userId  用户ID
     * @param fromUtc 起始时间（UTC）
     * @param type    周期天数（7 或 30）
     * @param env     目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求失败时抛出
     */
    public static BigDecimal[] cyclicalPnl(long userId, LocalDateTime fromUtc, int type, String env) throws Exception {
        return UtilityTools.queryCyclicalPnl(userId, fromUtc,
                "user_assets_statistics_swap_day_profit_and_loss", true, type, env);
    }

    /**
     * 查询合约账户所有币种 available 余额，按 Redis 最新价折算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 合约账户期末 USDT 总额
     */
    public static BigDecimal getCurrentSwapAccount(long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<SwapUserAssetsEntity> swapAssets = UserAssetsManage.querySwapAssets(userId);
        for (SwapUserAssetsEntity asset : swapAssets) {
            BigDecimal available = asset.getAvailable();
            if (available.compareTo(BigDecimal.ZERO) == 0) continue;
            total = total.add(available.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }
        logger.warn("合约账户 USDT总额：" + total);
        return total;
    }

    /**
     * 查询合约账户全部资产（可用余额 + 持仓未实现盈亏），折算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 合约账户 USDT 总额（含未实现盈亏）
     */
    public static BigDecimal getCurrentSwapTotal(long userId, String env) {
        BigDecimal account = getCurrentSwapAccount(userId, env);
        BigDecimal position = getSwapFrontPositionAmount(userId, env);
        logger.info("用户" + userId + "合约未实现盈亏：" + position);
        BigDecimal total = account.add(position);
        logger.warn("合约全部资产（账户+未实现盈亏）USDT：" + total);
        return total;
    }

    /**
     * 查询合约前端持仓（swap_position 表），按 Redis swap:new:price 计算未实现盈亏 USDT 总额。
     *
     * @param userId 用户ID
     * @return 合约前端仓位未实现盈亏 USDT 总额
     */
    public static BigDecimal getSwapFrontPositionAmount(long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<SwapFrontPositionEntity> swapFrontPositions = UserAssetsManage.querySwapFrontPositions(userId);
        for (SwapFrontPositionEntity item : swapFrontPositions) {
            BigDecimal newPrice = NewestPriceUtil.newPriceOfSwap(item.getSymbol(), env);
            BigDecimal costOpen = item.getCostOpen();
            int direction = item.getDirection();
            BigDecimal volume = BigDecimal.valueOf(item.getVolume());
            BigDecimal contractSize = item.getContractSize();
            total = total.add(UtilityTools.calcSwapPnl(direction, newPrice, costOpen, volume, contractSize));
        }
        logger.warn("合约未实现盈亏 USDT总额：" + total);
        return total;
    }

    /**
     * 汇总指定日期内合约账户划入金额（swap_transfer_order 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 合约划入 USDT 总额
     */
    public static BigDecimal getSwapTransferInTotal(long userId, String updateTime) {
        BigDecimal total = BigDecimal.ZERO;
        List<SwapTransferOrderEntity> swapTransferIns = UserAssetsManage.querySwapTransferIn(userId, updateTime);
        for (SwapTransferOrderEntity item : swapTransferIns) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        return total;
    }

    /**
     * 汇总指定日期内合约账户划出金额（swap_transfer_order 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 合约划出 USDT 总额
     */
    public static BigDecimal getSwapTransferOutTotal(long userId, String updateTime) {
        BigDecimal total = BigDecimal.ZERO;
        List<SwapTransferOrderEntity> swapTransferOuts = UserAssetsManage.querySwapTransferOut(userId, updateTime);
        for (SwapTransferOrderEntity item : swapTransferOuts) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        return total;
    }

    /**
     * 永续合约标的本地资产快照，用于与 /swap/assets assetList 逐字段断言对比。
     * 由 {@link #getSwapSymbolDetailMap} 统一构建。
     */
    public static class SwapSymbolDetail {
        public BigDecimal available = BigDecimal.ZERO; // 可划转余额
        public BigDecimal frozen = BigDecimal.ZERO; // 冻结余额（SwapUserAssetsEntity.frozen）
        public BigDecimal profitLoss = BigDecimal.ZERO; // 未实现盈亏（按 symbol 汇总持仓盈亏）
        public BigDecimal marginBalance = BigDecimal.ZERO; // 保证金余额；非USDT保证金账户（如ET）保持0
        public BigDecimal walletBalance = BigDecimal.ZERO; // available + frozen（post-loop 计算）
        public String currency = "";              // 保证金币种，来自 entity.getCurrency()
    }

    /**
     * 按合约标的（symbol）构建本地资产快照 map，与 /swap/assets assetList 对应。
     * key = symbol（原始大小写，如 "BTC_USD"），value = {@link SwapSymbolDetail}
     *
     * <ul>
     *   <li>available / frozen：来自 swap_user_assets 表，按 symbol 汇总</li>
     *   <li>profitLoss：来自 swap_all_position 表，按 symbol 汇总未实现盈亏（MySQL 大数据价格）</li>
     *   <li>marginBalance = (available + frozen) + profitLoss</li>
     * </ul>
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return symbol → SwapSymbolDetail
     */
    public static Map<String, SwapSymbolDetail> getSwapSymbolDetailMap(long userId, String env) {
        Map<String, SwapSymbolDetail> map = new HashMap<>();

        // 先查询是否有仓位
        List<SwapFrontPositionEntity> swapFrontPositions = UserAssetsManage.querySwapFrontPositions(userId);
        // 判断是否没有仓位
        boolean hasNoPosition = (swapFrontPositions == null || swapFrontPositions.isEmpty());

        // 1. 账户余额：available + frozen，按 API symbol 归集
        // key 全逐仓 key="usdt" 分仓key=交易对 平台币key="et"
        List<SwapUserAssetsEntity> swapAssets = UserAssetsManage.querySwapAssets(userId);
        for (SwapUserAssetsEntity a : swapAssets) {
            String key = a.getCurrency().equalsIgnoreCase("usdt")
                    ? a.getSymbol().toLowerCase()
                    : a.getCurrency().toLowerCase();
            SwapSymbolDetail d = map.computeIfAbsent(key, k -> new SwapSymbolDetail());
            d.frozen = d.frozen.add(a.getFrozen());
            d.currency = a.getCurrency().toLowerCase();
            d.walletBalance = d.walletBalance.add(a.getAvailable());
            d.available = d.available.add(a.getAvailable()).subtract(a.getFrozen());
        }

        // 2. 持仓未实现盈亏：持仓均为 USDT 保证金；可划转余额
        if (!hasNoPosition) {
            Integer mode = swapFrontPositions.get(0).getMode();
            String posKey;

            // 用于控制 mode 1 和 2 下的计算可划转余额的逻辑只执行一次
            boolean mode12Executed = false;

            for (SwapFrontPositionEntity p : swapFrontPositions) {
                BigDecimal newPrice = NewestPriceUtil.newPriceOfSwap(p.getSymbol(), env);
                BigDecimal costOpen = p.getCostOpen();
                int direction = p.getDirection();
                BigDecimal volume = BigDecimal.valueOf(p.getVolume());
                BigDecimal contractSize = p.getContractSize();
                String symbol = p.getSymbol();
                if (costOpen.compareTo(BigDecimal.ZERO) == 0) continue;
                if (direction != 1 && direction != 2) {
                    logger.warn("swap position direction 异常：" + direction + " symbol=" + p.getSymbol());
                    continue;
                }

                if (mode == 1 || mode == 2) {
                    posKey = "usdt";
                } else {
                    posKey = p.getSymbol().toLowerCase();
                }

                BigDecimal pnl = UtilityTools.calcSwapPnl(direction, newPrice, costOpen, volume, contractSize);
                map.computeIfAbsent(posKey, k -> new SwapSymbolDetail()).profitLoss =
                        map.get(posKey).profitLoss.add(pnl);

                if (mode == 1 || mode == 2) {
                    if (!mode12Executed) {
                        BigDecimal transferableAmount = transferableAmount(userId, symbol, env);
                        // map.computeIfAbsent(posKey, k -> new SwapSymbolDetail()).available =
                        //         map.get(posKey).available.add(transferableAmount);
                        map.computeIfAbsent(posKey, k -> new SwapSymbolDetail()).available =
                                transferableAmount;
                        // 执行完后立刻将标志位设为 true，后续循环不再执行
                        mode12Executed = true;
                    }
                } else if (mode == 3) {
                    BigDecimal transferableAmount = transferableAmount(userId, symbol, env);
                    // map.computeIfAbsent(posKey, k -> new SwapSymbolDetail()).available =
                    //         map.get(posKey).available.add(transferableAmount);
                    map.computeIfAbsent(posKey, k -> new SwapSymbolDetail()).available =
                            transferableAmount;
                }

            }
        }

        //  3.marginBalance 仅对 USDT 保证金账户计算，ET 等平台币接口返回 0，本地保持 ZERO
        for (SwapSymbolDetail d : map.values()) {
            if ("usdt".equals(d.currency)) {
                d.marginBalance = d.walletBalance.add(d.profitLoss);
            }
        }

        logger.info("SwapSymbolDetailMap size=" + map.size());
        return map;
    }

    /**
     * 可用划转余额 --账户维度
     * 全仓=保证金余额 - 仓位占用保证金 - 委托冻结的保证金 - 所有仓位维持保证金之和
     * 逐仓=保证金余额 - 仓位占用保证金 - 委托冻结的保证金
     * 分仓=保证金余额 - 仓位占用保证金 - 委托冻结的保证金 - 维持保证金  指定symbol
     */
    public static BigDecimal transferableAmount(Long userId, String tradeSymbol, String env) {
        SwapFrontUserConfigEntity configEntity = queryMode(userId);
        Integer mode = configEntity.getMode();
        BigDecimal marginBalance;
        BigDecimal totalInitialMargin;
        BigDecimal frozenMargin;
        BigDecimal totalMaintainMargin;
        if (mode == 1) {
            marginBalance = marginBalance(userId, null, env);
            totalInitialMargin = totalInitialMargin(userId, null);
            frozenMargin = queryFrozenMargin(userId, "usdt", null);
            totalMaintainMargin = maintainMarginOfAccount(userId, null);
            return marginBalance.subtract(totalInitialMargin).subtract(frozenMargin).subtract(totalMaintainMargin);
        } else if (mode == 2) {
            marginBalance = marginBalance(userId, null, env);
            totalInitialMargin = totalInitialMargin(userId, null);
            frozenMargin = queryFrozenMargin(userId, "usdt", null);
            return marginBalance.subtract(totalInitialMargin).subtract(frozenMargin);
        } else if (mode == 3) {
            marginBalance = marginBalance(userId, tradeSymbol, env);
            totalInitialMargin = totalInitialMargin(userId, tradeSymbol);
            frozenMargin = queryFrozenMargin(userId, "usdt", tradeSymbol);
            totalMaintainMargin = maintainMarginOfAccount(userId, tradeSymbol);
            return marginBalance.subtract(totalInitialMargin).subtract(frozenMargin).subtract(totalMaintainMargin);
        } else {
            Assert.assertEquals(1, 2, "用户" + userId + "仓位保证金模式参数不合法");
            return BigDecimal.ZERO;
        }
    }

    // 保证金余额 全仓&逐仓&分仓 账户维度
    public static BigDecimal marginBalance(Long userId, String tradeSymbol, String env) {
        BigDecimal marginBalance = BigDecimal.ZERO;
        SwapFrontUserConfigEntity configEntity = queryMode(userId);
        Integer mode = configEntity.getMode();
        if (mode == 1) {// 全仓
            // 钱包余额
            BigDecimal available = swapAccountAvailable(userId, "usdt", null);
            // 未实现盈亏
            BigDecimal unrealizedProfitOrLoss = unrealizedProfitOrLoss(userId, env);
            marginBalance = available.add(unrealizedProfitOrLoss);
        } else if (mode == 2) {
            // 钱包余额
            marginBalance = swapAccountAvailable(userId, "usdt", null);
        } else if (mode == 3) {
            // 钱包余额
            BigDecimal available = swapAccountAvailable(userId, "usdt", tradeSymbol);
            // 未实现盈亏
            BigDecimal unrealizedProfitOrLoss = unrealizedProfitOrLoss(userId, tradeSymbol, env);
            marginBalance = available.add(unrealizedProfitOrLoss);
        } else {
            logger.info("用户" + userId + "仓位保证金模式参数不合法");
        }
        logger.info("用户：" + userId + "保证金余额" + marginBalance);
        return marginBalance;
    }

    /**
     * 用户所有仓位的占用保证金
     * 全仓&逐仓：所有仓位初始保证金之和
     * 分仓：指定symbol 多空仓位初始保证金之和
     */
    public static BigDecimal totalInitialMargin(Long userId, String tradeSymbol) {
        BigDecimal totalInitialMargin = BigDecimal.ZERO;
        SwapFrontUserConfigEntity configEntity = queryMode(userId);
        Integer mode = configEntity.getMode();
        List<SwapFrontPositionEntity> swapFrontPositionEntities;
        if (mode == 1 || mode == 2) {
            swapFrontPositionEntities = queryPositions(userId, null, null);
        } else {
            swapFrontPositionEntities = queryPositions(userId, tradeSymbol, null);
        }
        for (SwapFrontPositionEntity entity : swapFrontPositionEntities) {
            BigDecimal initialMargin = initialMarginOfPosition(mode, entity);
            totalInitialMargin = totalInitialMargin.add(initialMargin);
        }
        logger.info("用户" + userId + "所有仓位占用保证金：" + totalInitialMargin);
        return totalInitialMargin;
    }

    // 委托冻结保证金
    public static BigDecimal queryFrozenMargin(Long userId, String currency, String tradeSymbol) {
        SwapFrontUserAssetsEntity swapFrontUserAssetsEntity = swapAccount(userId, currency, tradeSymbol);
        return swapFrontUserAssetsEntity.getFrozen();
    }

    /**
     * 账户的维持保证金
     * 全仓&逐仓：所有仓位的维持保证金之和
     * 分仓，同交易对，多空仓位的维持保证金之和 即账户的维持保证金 分仓！！！
     */
    public static BigDecimal maintainMarginOfAccount(Long userId, String tradeSymbol) {
        BigDecimal maintainMarginSum = BigDecimal.ZERO;
        List<SwapFrontPositionEntity> swapFrontPositionEntities;
        if (tradeSymbol == null || tradeSymbol.equals("") || tradeSymbol.equals("usdt")) {
            swapFrontPositionEntities = queryPositions(userId, null, null);
        } else {
            swapFrontPositionEntities = queryPositions(userId, tradeSymbol, null);
        }
        for (SwapFrontPositionEntity position : swapFrontPositionEntities) {
            BigDecimal maintainMarginOfPosition = maintainMarginOfPosition(position);
            maintainMarginSum = maintainMarginSum.add(maintainMarginOfPosition);
        }
        logger.info("用户" + userId + "账户的维持保证金：" + maintainMarginSum);
        return maintainMarginSum;
    }

    // 查用户合约保证金模式
    public static SwapFrontUserConfigEntity queryMode(Long userId) {
        return MyBatisUtil.execute(SwapFrontUserConfigEntityMapper.class, m -> m.selectByUserId(userId));
    }

    /**
     * 合约账户钱包余额
     * 全仓&逐仓 currency=usdt symbol=usdt
     * 分仓 currency=usdt symbol=交易对
     */
    public static BigDecimal swapAccountAvailable(Long userId, String currency, String symbol) {
        SwapFrontUserAssetsEntity swapFrontUserAssetsEntity = swapAccount(userId, currency, symbol);
        Assert.assertNotNull(swapFrontUserAssetsEntity, "用户" + userId + "无合约账户");
        BigDecimal available = swapFrontUserAssetsEntity.getAvailable();
        logger.info("用户" + userId + ",合约账户(全仓/逐仓)钱包余额：" + available);
        return available;
    }

    // 合约账户
    public static SwapFrontUserAssetsEntity swapAccount(Long userId, String currency, String tradeSymbol) {
        SwapFrontUserConfigEntity configEntity = queryMode(userId);
        Integer mode = configEntity.getMode();
        if (mode == 1 || mode == 2) {
            return MyBatisUtil.execute(SwapFrontUserAssetsEntityMapper.class, m -> m.selectByMode(userId, currency, null));
        } else {
            return MyBatisUtil.execute(SwapFrontUserAssetsEntityMapper.class, m -> m.selectByMode(userId, currency, tradeSymbol));
        }
    }

    // 合约 用户所有仓位未实现盈亏 全仓&逐仓
    public static BigDecimal unrealizedProfitOrLoss(Long userId, String env) {
        BigDecimal unrealizedProfitOrLoss;
        // 多仓
        List<SwapFrontPositionEntity> longPositionEntity
                = queryPositions(userId, null, 1);
        BigDecimal unrealizedProfitOrLossOfLongPosition = new BigDecimal("0.00000");
        for (SwapFrontPositionEntity position : longPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfLongPosition
                    = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "多仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfLongPosition);

        // 空仓
        List<SwapFrontPositionEntity> shortPositionEntity
                = queryPositions(userId, null, 2);
        BigDecimal unrealizedProfitOrLossOfShortPosition = new BigDecimal("0.00000");
        for (SwapFrontPositionEntity position : shortPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfShortPosition
                    = unrealizedProfitOrLossOfShortPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "空仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfShortPosition);

        // 该用户全部仓位未实现盈亏
        unrealizedProfitOrLoss = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfShortPosition);
        logger.info("用户" + userId + "全部仓位未实现盈亏共计：" + unrealizedProfitOrLoss);
        return unrealizedProfitOrLoss;
    }

    // 合约跟单 持仓未实现盈亏
    public static BigDecimal unrealizedProfitOrLossCopySwap(Long userId, String env) {
        BigDecimal unrealizedProfitOrLoss;
        // 多仓
        List<SwapFrontPositionEntity> longPositionEntity
                = queryCopyPositions(userId, null, 1);
        BigDecimal unrealizedProfitOrLossOfLongPosition = new BigDecimal("0.00000");
        for (SwapFrontPositionEntity position : longPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfLongPosition
                    = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "多仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfLongPosition);

        // 空仓
        List<SwapFrontPositionEntity> shortPositionEntity
                = queryCopyPositions(userId, null, 2);
        BigDecimal unrealizedProfitOrLossOfShortPosition = new BigDecimal("0.00000");
        for (SwapFrontPositionEntity position : shortPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfShortPosition
                    = unrealizedProfitOrLossOfShortPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "空仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfShortPosition);

        // 该用户全部仓位未实现盈亏
        unrealizedProfitOrLoss = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfShortPosition);
        logger.info("用户" + userId + "全部仓位未实现盈亏共计：" + unrealizedProfitOrLoss);
        return unrealizedProfitOrLoss;
    }

    // 计算单个仓位未实现盈亏
    public static BigDecimal unrealizedProfitOrLoss(SwapFrontPositionEntity position, String env) {
        Integer direction = position.getDirection();
        BigDecimal newPriceOfSwap = NewestPriceUtil.newPriceOfSwap(position.getSymbol(), env);
        BigDecimal averageOpeningPrice = position.getCostOpen();
        Long volume = position.getVolume();
        BigDecimal contractSize = position.getContractSize();
        return unrealizedProfitOrLoss(direction, newPriceOfSwap, averageOpeningPrice, volume, contractSize);
    }

    public static List<SwapFrontPositionEntity> queryPositions(Long userId, String tradeSymbol, Integer direction) {
        return MyBatisUtil.execute(SwapFrontPositionEntityMapper.class, m -> m.selectPositions(userId, tradeSymbol, direction));
    }

    public static List<SwapFrontPositionEntity> queryCopyPositions(Long userId, String tradeSymbol, Integer direction) {
        return MyBatisUtil.execute(SwapFrontPositionEntityMapper.class, m -> m.selectCopyPositions(userId, tradeSymbol, direction));
    }

    // 用户所有仓位未实现盈亏 全仓&逐仓&分仓 分仓需指定币对
    public static BigDecimal unrealizedProfitOrLoss(Long userId, String tradeSymbol, String env) {
        BigDecimal unrealizedProfitOrLoss;
        // 多仓
        List<SwapFrontPositionEntity> longPositionEntity;
        // 空仓
        List<SwapFrontPositionEntity> shortPositionEntity;
        BigDecimal unrealizedProfitOrLossOfLongPosition = new BigDecimal("0.00000");
        BigDecimal unrealizedProfitOrLossOfShortPosition = new BigDecimal("0.00000");
        if (tradeSymbol == null || tradeSymbol.equals("") || tradeSymbol.equals("usdt")) {
            longPositionEntity = queryPositions(userId, null, 1);
            shortPositionEntity = queryPositions(userId, null, 2);
        } else {
            longPositionEntity = queryPositions(userId, tradeSymbol, 1);
            shortPositionEntity = queryPositions(userId, tradeSymbol, 2);
        }
        for (SwapFrontPositionEntity position : longPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfLongPosition
                    = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "交易对" + tradeSymbol + "多仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfLongPosition);

        for (SwapFrontPositionEntity position : shortPositionEntity) {
            BigDecimal unrealizedProfitOrLossOfSymbol
                    = unrealizedProfitOrLoss(position, env);
            unrealizedProfitOrLossOfShortPosition
                    = unrealizedProfitOrLossOfShortPosition.add(unrealizedProfitOrLossOfSymbol);
        }
        logger.info("用户" + userId + "交易对" + tradeSymbol + "空仓仓位未实现盈亏共计：" + unrealizedProfitOrLossOfShortPosition);

        // 该用户全部仓位未实现盈亏
        unrealizedProfitOrLoss = unrealizedProfitOrLossOfLongPosition.add(unrealizedProfitOrLossOfShortPosition);
        logger.info("用户" + userId + "交易对" + tradeSymbol + "全部仓位未实现盈亏共计：" + unrealizedProfitOrLoss);
        return unrealizedProfitOrLoss;
    }

    /**
     * 计算单个仓位未实现盈亏
     * 多仓：未实现盈亏=（最新价-开仓均价）*（持仓张数*面值）/开仓均价
     * 空仓：未实现盈亏=（开仓均价-最新价）*（持仓张数*面值）/开仓均价
     */
    public static BigDecimal unrealizedProfitOrLoss(int direction, BigDecimal newestPrice, BigDecimal averageOpeningPrice, Long volume, BigDecimal contractSize) {
        BigDecimal unrealizedProfitOrLoss = BigDecimal.ZERO;
        if (direction == 1) {
            unrealizedProfitOrLoss = (newestPrice.subtract(averageOpeningPrice))
                    .multiply(BigDecimal.valueOf(volume)).multiply(contractSize)
                    .divide(averageOpeningPrice, 5, RoundingMode.HALF_UP);
        } else if (direction == 2) {
            unrealizedProfitOrLoss = (averageOpeningPrice.subtract(newestPrice))
                    .multiply(BigDecimal.valueOf(volume)).multiply(contractSize)
                    .divide(averageOpeningPrice, 5, RoundingMode.HALF_UP);
        } else {
            logger.info("开仓方向参数不合法：" + direction);
        }
        return unrealizedProfitOrLoss;
    }

    /**
     * 单个仓位初始保证金
     * 全仓&分仓=张数*面值/杠杆倍数
     * 逐仓=张数*面值/杠杆倍数 - 手续费  直接查库
     */
    public static BigDecimal initialMarginOfPosition(int mode, SwapFrontPositionEntity positionEntity) {
        if (mode == 1 || mode == 3) {
            // 持仓量
            Long volume = positionEntity.getVolume();
            // 面值
            BigDecimal contractSize = positionEntity.getContractSize();
            // 杠杆倍数
            int leverage = positionEntity.getLeverage();
            return BigDecimal.valueOf(volume).multiply(contractSize).divide(BigDecimal.valueOf(leverage), 2, RoundingMode.HALF_UP);
        } else {
            return positionEntity.getMargin();
        }
    }

    /**
     * 单个仓位维持保证金=仓位张数 * 面值 * 维持保证金率 - 该层级维持保证金速算额
     */
    public static BigDecimal maintainMarginOfPosition(SwapFrontPositionEntity positionEntity) {
        // 持仓量
        long volume = positionEntity.getVolume();
        // 标的
        String symbol = positionEntity.getSymbol();
        // 面值
        BigDecimal contractSize = positionEntity.getContractSize();
        // 维持保证金率
        SwapMarginEntity tier = MaintainMarginConfiguration.queryTier(symbol, volume);
        BigDecimal maintainMarginRate = tier.getRatio();
        // 维持保证金速算额
        BigDecimal maintainMarginQuickDeductionAmount = tier.getMargin();
        // 维持保证金
        BigDecimal maintainMargin = BigDecimal.valueOf(volume)
                .multiply(contractSize)
                .multiply(maintainMarginRate)
                .subtract(maintainMarginQuickDeductionAmount);
        logger.info(positionEntity.getId() + "仓位维持保证金：" + maintainMargin);
        return maintainMargin;
    }

}
