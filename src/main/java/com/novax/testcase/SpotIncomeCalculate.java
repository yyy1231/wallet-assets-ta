package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.*;
import com.novax.util.MongoUtils;
import com.novax.util.NewestPriceUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 现货主板区账户盈亏分析：支持今日、近7日、近30日盈亏计算，返回盈亏值供测试类断言。
 */
public class SpotIncomeCalculate extends BaseTest {

    /**
     * 现货主板区盈亏计算入口。
     *
     * @param userId 用户ID
     * @param type   校验类型：1=今日，7=近7日，其他=近30日
     * @param env    目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] getCyclicalPnL(long userId, int type, String env) throws Exception {
        if (type == 1) return spotTodayPnL(userId, env);
        else if (type == 7) return spotSevenPnL(userId, env);
        else return spotThirtyPnL(userId, env);
    }

    /**
     * 计算现货主板区今日盈亏。
     * 公式：今日盈亏 = 期末 - 期初 - (充值 - 提现) - (转入 - 转出) - (跟单转入 + 跟单转出)
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [今日盈亏USDT, 今日盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] spotTodayPnL(long userId, String env) throws Exception {
        String todayStr = today.format(DATE_FMT);

        BigDecimal end = getCurrentSpotAccount(userId, env);
        logger.info("期末资产：" + end);

        BigDecimal start = BigDecimal.ZERO;
        MongoCollection<Document> dayCol = MongoUtils.getMongoConnection(
                "superex", "user_assets_statistics_spot_day", env);
        BasicDBObject dayFilter = new BasicDBObject("user_id", userId)
                .append("create_time", UtilityTools.toDate(todayUtc));
        List<Document> dayDocs = UtilityTools.queryMongo(dayCol, dayFilter, null);
        if (dayDocs.size() == 1) {
            start = UtilityTools.toBigDecimal(dayDocs.get(0).get("total_usdt"));
        }
        logger.info("期初资产：" + start);

        BigDecimal chargeAmount = getTotalChargeAmount(userId, todayStr, env);
        logger.info("充值：" + chargeAmount);

        BigDecimal withdrawAmount = getTotalWithdrawAmount(userId, todayStr, env);
        logger.info("提现：" + withdrawAmount);

        BigDecimal transferIn = getSpotTransferInTotal(userId, todayStr);
        logger.info("转入：" + transferIn);

        BigDecimal transferInFromSwapCopy = getCopyTradingTransferIn(userId, todayStr, env);
        logger.info("从合约跟单账户转入：" + transferInFromSwapCopy);

        BigDecimal transferOut = getSpotTransferOutTotal(userId, todayStr);
        logger.info("转出：" + transferOut);

        BigDecimal transferOutToSwapCopy = getCopyTradingTransferOut(userId, todayStr, env);
        logger.info("转出至合约跟单账户：" + transferOutToSwapCopy);

        BigDecimal todayEarnings = end.subtract(start)
                .subtract(chargeAmount.subtract(withdrawAmount))
                .subtract(transferIn.subtract(transferOut))
                .subtract(transferInFromSwapCopy.add(transferOutToSwapCopy));
        logger.info("今日盈亏：" + todayEarnings);

        BigDecimal divisor = start.add(chargeAmount).add(transferIn).add(transferInFromSwapCopy);

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
     * 计算现货主板区近7日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近7日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] spotSevenPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, sevenUtc, 7, env);
    }

    /**
     * 计算现货主板区近30日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近30日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] spotThirtyPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, thirtyUtc, 30, env);
    }

    /**
     * 从 MongoDB 查询现货主板区周期盈亏。
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
                "user_assets_statistics_spot_day_profit_and_loss", false, type, env);
    }

    /**
     * 汇总现货账户（spot + ET奖励 + SCS奖励）期末资产，以 Redis 最新价折算为 USDT 总额。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 现货账户期末 USDT 总额
     */
    public static BigDecimal getCurrentSpotAccount(long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;

        List<SpotUserAssetsEntity> spotAssets = UserAssetsManage.querySpotAssets(userId);
        for (SpotUserAssetsEntity asset : spotAssets) {
            BigDecimal amount = asset.getAvailable().add(asset.getFrozen());
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<TaskEtRewardsEntity> etRewards = UserAssetsManage.queryEtRewards(userId);
        for (TaskEtRewardsEntity reward : etRewards) {
            BigDecimal amount = reward.getTotalFreeze().subtract(reward.getUnfreeze());
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(reward.getCurrency() + "_usdt", env)));
        }

        List<ActivityUserRewardsEntity> scsRewards = UserAssetsManage.queryScsRewards(userId);
        for (ActivityUserRewardsEntity reward : scsRewards) {
            BigDecimal amount = reward.getTotalFreeze().subtract(reward.getUnfreeze());
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(reward.getCurrency() + "_usdt", env)));
        }

        logger.warn("现货账户（spot+ET+SCS）USDT总额：" + total);
        return total;
    }

    /**
     * 汇总指定日期内充值总额：法币充值 + 内部划转充值。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 充值 USDT 总额
     */
    public static BigDecimal getTotalChargeAmount(long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyChargeOrders = UserAssetsManage.queryUserCurrencyChargeOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyChargeOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<InternalTransferOrderEntity> internalTransferOrders = UserAssetsManage.queryInternalTransferOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferOrders) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        return total;
    }

    /**
     * 汇总指定日期内提现总额：法币提现 + 内部划转提现。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 提现 USDT 总额
     */
    public static BigDecimal getTotalWithdrawAmount(long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyWithdrawOrders = UserAssetsManage.queryUserCurrencyWithdrawOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyWithdrawOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<InternalTransferOrderEntity> internalTransferWithdrawOrders = UserAssetsManage.queryInternalTransferWithdrawOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferWithdrawOrders) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        return total;
    }

    /**
     * 汇总指定日期内现货账户划入金额（spot_transfer_order 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 现货划入 USDT 总额
     */
    public static BigDecimal getSpotTransferInTotal(long userId, String updateTime) {
        BigDecimal total = BigDecimal.ZERO;
        List<SpotTransferOrderEntity> spotTransferIns = UserAssetsManage.querySpotTransferIn(userId, updateTime);
        for (SpotTransferOrderEntity item : spotTransferIns) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        return total;
    }

    /**
     * 汇总指定日期内现货账户划出金额（spot_transfer_order 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 现货划出 USDT 总额
     */
    public static BigDecimal getSpotTransferOutTotal(long userId, String updateTime) {
        BigDecimal total = BigDecimal.ZERO;
        List<SpotTransferOrderEntity> spotTransferOuts = UserAssetsManage.querySpotTransferOut(userId, updateTime);
        for (SpotTransferOrderEntity item : spotTransferOuts) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        return total;
    }

    /**
     * 汇总指定日期内跟单划入金额（spot_finance_record 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @return 跟单划入 USDT 总额
     */
    public static BigDecimal getCopyTradingTransferIn(long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<SpotFinanceRecordQueryEntity> copyTradingTransferIns = UserAssetsManage.queryCopyTradingTransferIn(userId, updateTime);
        for (SpotFinanceRecordQueryEntity item : copyTradingTransferIns) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        return total;
    }

    /**
     * 汇总指定日期内跟单划出金额（spot_finance_record 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 跟单划出 USDT 总额
     */
    public static BigDecimal getCopyTradingTransferOut(long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<SpotFinanceRecordQueryEntity> copyTradingTransferOuts = UserAssetsManage.queryCopyTradingTransferOut(userId, updateTime);
        for (SpotFinanceRecordQueryEntity item : copyTradingTransferOuts) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        return total;
    }

}
