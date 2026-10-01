package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.MultiCurrencySwapPositionEntity;
import com.novax.entity.mysql.MultiCurrencySwapUserAssetsEntity;
import com.novax.entity.mysql.TransferOrderEntity;
import com.novax.util.MongoUtils;
import com.novax.util.NewestPriceUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 全币种合约账户盈亏分析：支持今日、近7日、近28日盈亏计算，返回盈亏值供测试类断言。
 */
public class MultiCurrencySwapIncomeCalculate extends BaseTest {
    /** 全币种合约使用近28天（覆盖日历4周），与其他账户的31天不同 */
    private static final LocalDateTime thirtyUtc = today.minusDays(28).minusHours(8).truncatedTo(ChronoUnit.HOURS);

    /**
     * 全币种合约账户盈亏计算入口。
     *
     * @param userId 用户ID
     * @param type   校验类型：1=今日，7=近7日，其他=近28日
     * @param env    目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] getCyclicalPnL(long userId, int type, String env) throws Exception {
        if (type == 1)      return multiCurrencySwapTodayPnL(userId, env);
        else if (type == 7) return multiCurrencySwapSevenPnL(userId, env);
        else                return multiCurrencySwapThirtyPnL(userId, env);
    }

    /**
     * 计算全币种合约账户今日盈亏。
     * 公式：今日盈亏 = 期末 - 期初 - (转入 - 转出) - 期初浮动 + 未实现盈亏
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [今日盈亏USDT, 今日盈亏率]}
     */
    public static BigDecimal[] multiCurrencySwapTodayPnL(long userId, String env) {
        BigDecimal end = getMultiCurrencySwapAmount(userId, env);
        logger.info("期末资产：" + end);

        BigDecimal start = BigDecimal.ZERO;
        BigDecimal floatBegin = BigDecimal.ZERO;
        MongoCollection<Document> dayCol = MongoUtils.getMongoConnection(
                "superex", "user_assets_statistics_multi_swap_day", env);
        BasicDBObject dayFilter = new BasicDBObject("user_id", userId)
                .append("create_time", UtilityTools.toDate(todayUtc));
        List<Document> dayDocs = UtilityTools.queryMongo(dayCol, dayFilter, null);
        if (dayDocs.size() == 1) {
            start = UtilityTools.toBigDecimal(dayDocs.get(0).get("total_usdt"));
            floatBegin = UtilityTools.toBigDecimal(dayDocs.get(0).get("position_usdt"));
        }
        logger.info("期初资产：" + start);
        logger.info("期初浮动：" + floatBegin);

        BigDecimal transferIn = getTransferAmount(userId, 7, today.format(DATE_FMT));
        logger.info("转入：" + transferIn);
        BigDecimal transferOut = getTransferAmount(userId, 8, today.format(DATE_FMT));
        logger.info("转出：" + transferOut);

        BigDecimal positionAmount = getMultiCurrencySwapPositionAmount(userId, env);
        logger.info("未实现盈亏(全币种)：" + positionAmount);

        BigDecimal todayEarnings = end.subtract(start)
                .subtract(transferIn.subtract(transferOut))
                .subtract(floatBegin)
                .add(positionAmount);
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
     * 计算全币种合约账户近7日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近7日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] multiCurrencySwapSevenPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, sevenUtc, 7, env);
    }

    /**
     * 计算全币种合约账户近28日盈亏（全币种合约专用周期，覆盖日历4周）。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近28日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] multiCurrencySwapThirtyPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, thirtyUtc, 30, env);
    }

    /**
     * 从 MongoDB 查询全币种合约账户周期盈亏。
     *
     * @param userId  用户ID
     * @param fromUtc 起始时间（UTC）
     * @param type    周期标签（7 或 30，仅用于日志/断言提示）
     * @param env     目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求失败时抛出
     */
    public static BigDecimal[] cyclicalPnl(long userId, LocalDateTime fromUtc, int type, String env) throws Exception {
        return UtilityTools.queryCyclicalPnl(userId, fromUtc,
                "user_assets_statistics_multi_swap_day_profit_and_loss", true, type, env);
    }

    /**
     * 查询全币种合约账户所有币种 available 余额，按 Redis 最新价折算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 全币种账户期末 USDT 总额
     */
    public static BigDecimal getMultiCurrencySwapAmount(long userId, String env) {
        List<MultiCurrencySwapUserAssetsEntity> assets = UserAssetsManage.queryMultiCurrencySwapAssets(userId);
        BigDecimal usdtAmount = BigDecimal.ZERO;
        for (MultiCurrencySwapUserAssetsEntity asset : assets) {
            BigDecimal available = asset.getAvailable();
            usdtAmount = usdtAmount.add(available.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }
        logger.warn("全币种账户 USDT总额：" + usdtAmount);
        return usdtAmount;
    }

    /**
     * 查询全币种合约账户全部资产（可用余额 + 持仓未实现盈亏），折算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 全币种合约账户 USDT 总额（含未实现盈亏）
     */
    public static BigDecimal getCurrentMultiCurrencySwapTotal(long userId, String env) {
        BigDecimal account  = getMultiCurrencySwapAmount(userId, env);
        BigDecimal position = getMultiCurrencySwapPositionAmount(userId, env);
        BigDecimal total = account.add(position);
        logger.warn("全币种合约全部资产（账户+未实现盈亏）USDT：" + total);
        return total;
    }

    /**
     * 查询全币种合约持仓，按 Redis swap:index:price 计算未实现盈亏并换算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 全币种合约持仓未实现盈亏 USDT 总额
     */
    public static BigDecimal getMultiCurrencySwapPositionAmount(long userId, String env) {
        List<MultiCurrencySwapPositionEntity> positions = UserAssetsManage.queryMultiCurrencySwapPositions(userId);
        BigDecimal usdtAmount = BigDecimal.ZERO;
        for (MultiCurrencySwapPositionEntity pos : positions) {
            String currency = pos.getCurrency();
            String symbol = pos.getSymbol().replace(currency, "usdt");
            BigDecimal newPrice = NewestPriceUtil.getSwapIndexPrice(symbol, env);
            BigDecimal costOpen = pos.getCostOpen();
            BigDecimal volume = pos.getVolume();
            int direction = pos.getDirection();
            BigDecimal profit = UtilityTools.calcMultiSwapPnl(direction, newPrice, costOpen, volume);
            BigDecimal currencyPrice = NewestPriceUtil.getSwapIndexPrice(currency + "_usdt", env);
            usdtAmount = usdtAmount.add(profit.multiply(currencyPrice));
        }
        logger.warn("全币种合约未实现盈亏 USDT总额：" + usdtAmount);
        return usdtAmount;
    }

    /**
     * 按 transfer_order.type 查询指定日期内划转总额（USDT）。
     * type=7 表示转入全币种合约，type=8 表示转出全币种合约。
     *
     * @param userId     用户ID
     * @param typeIn     划转类型（transfer_order.type 字段值）
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 指定类型划转 USDT 总额
     */
    public static BigDecimal getTransferAmount(long userId, int typeIn, String updateTime) {
        List<TransferOrderEntity> data = UserAssetsManage.queryTransferByType(userId, typeIn, updateTime);
        BigDecimal amount = BigDecimal.ZERO;
        for (TransferOrderEntity item : data) {
            amount = amount.add(item.getAmount().multiply(item.getNewPrice()));
        }
        return amount;
    }

}
