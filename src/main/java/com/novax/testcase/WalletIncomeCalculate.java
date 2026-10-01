package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.*;
import com.novax.util.MongoUtils;
import com.novax.util.NewestPriceUtil;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 钱包总览盈亏分析：汇总全量账户（现货+合约+P2P+理财+奖励+网格+跟单+全币种合约），
 * 支持今日、近7日、近30日盈亏校验，与大数据接口返回值断言对比。
 */
public class WalletIncomeCalculate extends BaseTest {
    public static Logger logger = Logger.getLogger(WalletIncomeCalculate.class);

    /**
     * 钱包总览盈亏校验入口。
     *
     * @param userId 用户ID
     * @param type   校验类型：1=今日，7=近7日，其他=近30日
     * @param env    目标环境（test/dev）
     * @return {@code [盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] getCyclicalPnL(long userId, int type, String env) throws Exception {
        if (type == 1) return walletTodayPnL(userId, env);
        else if (type == 7) return walletSevenPnL(userId, env);
        else return walletThirtyPnL(userId, env);
    }

    /**
     * 计算钱包总览今日盈亏。
     * 公式：今日盈亏 = 期末 - 期初 - (充值 - 提现) + 合约未实现盈亏(基础+跟单) - 期初浮动 + 全币种合约未实现盈亏
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [今日盈亏USDT, 今日盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] walletTodayPnL(Long userId, String env) throws Exception {
        String todayStr = today.format(DATE_FMT);

        // 期末：全量钱包账户（现货+合约+P2P+理财+ET+SCS+自由区+网格+跟单+全币种合约），不含未实现盈亏
        BigDecimal end = getCurrentWalletAccount(userId, env);
        logger.info("期末资产：" + end);

        // 期初资产 + 期初浮动盈亏（合约基础+跟单+全币种合约）：MongoDB user_assets_statistics_wallet_day
        BigDecimal start = BigDecimal.ZERO;
        BigDecimal floatBegin = BigDecimal.ZERO;
        MongoCollection<Document> dayCol = MongoUtils.getMongoConnection(
                "superex", "user_assets_statistics_wallet_day", env);
        BasicDBObject dayFilter = new BasicDBObject("user_id", userId)
                .append("create_time", UtilityTools.toDate(todayUtc));
        List<Document> dayDocs = UtilityTools.queryMongo(dayCol, dayFilter, null);
        if (dayDocs.size() == 1) {
            start = UtilityTools.toBigDecimal(dayDocs.get(0).get("total_usdt"));
            floatBegin = UtilityTools.toBigDecimal(dayDocs.get(0).get("position_usdt"));
        }
        logger.info("期初资产：" + start + "，期初浮动：" + floatBegin);

        // 充值（含自由区）
        BigDecimal chargeAmount = getTotalChargeAmountWithFree(userId, todayStr, env);
        logger.info("充值：" + chargeAmount);

        // 提现（含自由区）
        BigDecimal withdrawAmount = getTotalWithdrawAmountWithFree(userId, todayStr, env);
        logger.info("提现：" + withdrawAmount);

        // 未实现盈亏：合约基础+跟单
        BigDecimal position = getSwapPositionAmount(userId, env);
        logger.info("合约未实现盈亏：" + position);

        // 未实现盈亏：全币种合约（Redis swap:index:price）
        BigDecimal positionAmount = getMultiCurrencySwapPositionAmount(userId, env);
        logger.info("全币种合约未实现盈亏：" + positionAmount);

        // 今日盈亏 = 期末 - 期初 - (充值 - 提现) + 合约盈亏 - 期初浮动 + 全币种合约盈亏
        BigDecimal todayEarnings = end.subtract(start)
                .subtract(chargeAmount.subtract(withdrawAmount))
                .add(position)
                .subtract(floatBegin)
                .add(positionAmount);
        logger.info("今日盈亏：" + todayEarnings);

        // 今日盈亏率 = 今日盈亏 / (期初 + 期初浮动 + 充值) × 100
        BigDecimal divisor = start.add(floatBegin).add(chargeAmount);
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
        logger.info("钱包总览盈亏：" + todayEarnings + " >>> 钱包总览盈亏率：" + todayEarningsRatio);
        return new BigDecimal[]{todayEarnings, todayEarningsRatio};
    }

    /**
     * 计算钱包总览近7日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] walletSevenPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, sevenUtc, 7, env);
    }

    /**
     * 计算钱包总览近30日盈亏。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return {@code [近30日盈亏USDT, 盈亏率]}
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] walletThirtyPnL(long userId, String env) throws Exception {
        return cyclicalPnl(userId, thirtyUtc, 30, env);
    }

    /**
     * 从 MongoDB 查询钱包总览周期盈亏，并与接口返回值断言对比。
     *
     * @param userId  用户ID
     * @param fromUtc 起始时间（UTC）
     * @param type    周期天数（7 或 30）
     * @param env     目标环境（test/dev）
     * @throws Exception HTTP 请求或断言失败时抛出
     */
    public static BigDecimal[] cyclicalPnl(long userId, LocalDateTime fromUtc, int type, String env) throws Exception {
        return UtilityTools.queryCyclicalPnl(userId, fromUtc,
                "user_assets_statistics_wallet_day_profit_and_loss", true, type, env);
    }

    /**
     * 汇总全量钱包账户期末资产（现货+合约+P2P+理财利息+理财持仓+ET奖励+SCS奖励+自由区+网格+跟单+全币种合约），
     * 均以 Redis 最新价（spot:new:price）折算为 USDT。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 钱包总览期末 USDT 总额
     */
    public static BigDecimal getCurrentWalletAccount(Long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;

        List<SpotUserAssetsEntity> spotAssets = UserAssetsManage.querySpotAssets(userId);
        for (SpotUserAssetsEntity asset : spotAssets) {
            BigDecimal amount = asset.getAvailable().add(asset.getFrozen());
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<SwapUserAssetsEntity> swapAssets = UserAssetsManage.querySwapAssets(userId);
        for (SwapUserAssetsEntity asset : swapAssets) {
            BigDecimal available = asset.getAvailable();
            total = total.add(available.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<OtcUserAssetsEntity> otcAssets = UserAssetsManage.queryOtcAssets(userId);
        for (OtcUserAssetsEntity asset : otcAssets) {
            BigDecimal amount = asset.getAvailable().add(asset.getCashDeposit()).add(asset.getFrozen());
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<FinanceAccountEntity> financeAccounts = UserAssetsManage.queryFinanceAccounts(userId);
        for (FinanceAccountEntity asset : financeAccounts) {
            BigDecimal usdtVal = asset.getBalance().multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env));
            total = total.add(NewestPriceUtil.toBtcAmount(usdtVal, env));
        }

        List<FinanceDepositEntity> activeFinanceDeposits = UserAssetsManage.queryActiveFinanceDeposits(userId);
        for (FinanceDepositEntity asset : activeFinanceDeposits) {
            total = total.add(asset.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
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

        List<FreeTradeSpotUserAssetsEntity> freeTradeSpotAssets = UserAssetsManage.queryFreeTradeSpotAssets(userId);
        for (FreeTradeSpotUserAssetsEntity asset : freeTradeSpotAssets) {
            BigDecimal amount = asset.getAvailable().add(asset.getFrozen());
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<GridSpotUserAssetsEntity> gridSpotAssets = UserAssetsManage.queryGridSpotAssets(userId);
        for (GridSpotUserAssetsEntity asset : gridSpotAssets) {
            BigDecimal amount = asset.getAvailable().add(asset.getFrozen());
            total = total.add(amount.multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<SwapCopyUserAssetsEntity> swapCopyAssets = UserAssetsManage.querySwapCopyAssets(userId);
        for (SwapCopyUserAssetsEntity asset : swapCopyAssets) {
            total = total.add(asset.getAvailable().multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        List<MultiCurrencySwapUserAssetsEntity> multiCurrencySwapAssets = UserAssetsManage.queryMultiCurrencySwapAssets(userId);
        for (MultiCurrencySwapUserAssetsEntity asset : multiCurrencySwapAssets) {
            total = total.add(asset.getAvailable().multiply(NewestPriceUtil.newPriceOfSymbol(asset.getCurrency() + "_usdt", env)));
        }

        logger.info("钱包总览期末资产 USDT：" + total);
        return total;
    }

    /**
     * 钱包总资产 BTC 折算（Redis BTC/USDT 价格）
     */
    public static BigDecimal getCurrentWalletTotalBtc(BigDecimal totalUsdt, String env) {
        return NewestPriceUtil.toBtcAmount(totalUsdt, "usdt", env);
    }

    /**
     * 钱包资产汇总 账户+持仓浮动盈亏
     */
    public static BigDecimal getCurrentWalletTotal(Long userId, String env) {
        // 汇总账户资产
        BigDecimal currentWalletAccountAssets = getCurrentWalletAccount(userId, env);
        // 持仓浮动
        BigDecimal swapPositionAmount = getSwapPositionAmount(userId, env);
        BigDecimal multiCurrencySwapPositionAmount = getMultiCurrencySwapPositionAmount(userId, env);
        BigDecimal totalU = currentWalletAccountAssets.add(swapPositionAmount).add(multiCurrencySwapPositionAmount);
        logger.info("钱包总览期末资产 USDT：" + totalU);
        return totalU;
    }

    /**
     * 汇总指定日期内充值总额（含自由区）：法币充值 + 内部划转充值 + 自由区链上充值 + 自由区内部充值。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 充值 USDT 总额
     */
    public static BigDecimal getTotalChargeAmountWithFree(Long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyChargeOrders = UserAssetsManage.queryUserCurrencyChargeOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyChargeOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<InternalTransferOrderEntity> internalTransferOrders = UserAssetsManage.queryInternalTransferOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferOrders) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        List<FreeTradeChainTransferOrderEntity> freeTradeChainTransferOrders = UserAssetsManage.queryFreeTradeChainTransferOrders(userId, updateTime);
        for (FreeTradeChainTransferOrderEntity item : freeTradeChainTransferOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<FreeTradeInternalTransferOrderEntity> freeTradeInternalTransferOrders = UserAssetsManage.queryFreeTradeInternalTransferOrders(userId, updateTime);
        for (FreeTradeInternalTransferOrderEntity item : freeTradeInternalTransferOrders) {
            total = total.add(item.getUsdtTotal());
        }
        return total;
    }

    /**
     * 汇总指定日期内提现总额（含自由区）：法币提现 + 内部划转提现 + 自由区链上提现 + 自由区内部提现。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd
     * @param env        目标环境（test/dev）
     * @return 提现 USDT 总额
     */
    public static BigDecimal getTotalWithdrawAmountWithFree(Long userId, String updateTime, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<UserCurrencyOrderQueryEntity> userCurrencyWithdrawOrders = UserAssetsManage.queryUserCurrencyWithdrawOrders(userId, updateTime);
        for (UserCurrencyOrderQueryEntity item : userCurrencyWithdrawOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<InternalTransferOrderEntity> internalTransferWithdrawOrders = UserAssetsManage.queryInternalTransferWithdrawOrders(userId, updateTime);
        for (InternalTransferOrderEntity item : internalTransferWithdrawOrders) {
            total = total.add(item.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(item.getCurrency() + "_usdt", env)));
        }
        List<FreeTradeChainTransferOrderEntity> freeTradeChainWithdrawOrders = UserAssetsManage.queryFreeTradeChainWithdrawOrders(userId, updateTime);
        for (FreeTradeChainTransferOrderEntity item : freeTradeChainWithdrawOrders) {
            total = total.add(item.getAmount().multiply(item.getNewPrice()));
        }
        List<FreeTradeInternalTransferOrderEntity> freeTradeInternalWithdrawOrders = UserAssetsManage.queryFreeTradeInternalWithdrawOrders(userId, updateTime);
        for (FreeTradeInternalTransferOrderEntity item : freeTradeInternalWithdrawOrders) {
            total = total.add(item.getUsdtTotal());
        }
        return total;
    }

    /**
     * 查询合约基础+跟单账户全量持仓（swap_all_position 表），按 Redis swap:new:price 计算未实现盈亏 USDT 总额。
     *
     * @param userId 用户ID
     * @return 合约基础+跟单未实现盈亏 USDT 总额
     */
    public static BigDecimal getSwapPositionAmount(Long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;
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
            total = total.add(UtilityTools.calcSwapPnl(direction, newPrice, costOpen, volume, contractSize));
        }
        logger.warn("合约未实现盈亏（基础+跟单）USDT：" + total);
        return total;
    }

    /**
     * 按币种维度汇总全量钱包账户的 USDT 资产，与 /user-asset/currency/total 接口 assetList 对应。
     * key = 币种小写（如 "usdt"、"btc"），value = 该币种折合 USDT 总额。
     * 价格来源与 {@link #getCurrentWalletAccount} 完全一致，持仓浮盈不计入。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return 币种 → USDT 总额 map
     */
    public static Map<String, WalletCurrencyDetail> getWalletCurrencyMap(long userId, String env) {
        Map<String, WalletCurrencyDetail> map = new HashMap<>();

        // 现货：available + frozen，Redis 价格
        List<SpotUserAssetsEntity> spotAssets = UserAssetsManage.querySpotAssets(userId);
        for (SpotUserAssetsEntity a : spotAssets) {
            BigDecimal amt = a.getAvailable().add(a.getFrozen());
            if (amt.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // U 本位合约：available，Redis 价格
        List<SwapUserAssetsEntity> swapAssets = UserAssetsManage.querySwapAssets(userId);
        for (SwapUserAssetsEntity a : swapAssets) {
            BigDecimal amt = a.getAvailable();
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // C2C（OTC）：available + cashDeposit + frozen，Redis 价格
        List<OtcUserAssetsEntity> otcAssets = UserAssetsManage.queryOtcAssets(userId);
        for (OtcUserAssetsEntity a : otcAssets) {
            BigDecimal amt = a.getAvailable().add(a.getCashDeposit()).add(a.getFrozen());
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 理财活期余额：Redis 价格
        List<FinanceAccountEntity> financeAccounts = UserAssetsManage.queryFinanceAccounts(userId);
        for (FinanceAccountEntity a : financeAccounts) {
            BigDecimal amt = a.getBalance();
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 理财定期持仓：Redis 价格
        List<FinanceDepositEntity> activeFinanceDeposits = UserAssetsManage.queryActiveFinanceDeposits(userId);
        for (FinanceDepositEntity a : activeFinanceDeposits) {
            BigDecimal amt = a.getAmount();
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // ET 奖励：totalFreeze - unfreeze，Redis 价格
        List<TaskEtRewardsEntity> etRewards = UserAssetsManage.queryEtRewards(userId);
        for (TaskEtRewardsEntity a : etRewards) {
            BigDecimal amt = a.getTotalFreeze().subtract(a.getUnfreeze());
            if (amt.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // SCS 奖励：totalFreeze - unfreeze，Redis 价格
        List<ActivityUserRewardsEntity> scsRewards = UserAssetsManage.queryScsRewards(userId);
        for (ActivityUserRewardsEntity a : scsRewards) {
            BigDecimal amt = a.getTotalFreeze().subtract(a.getUnfreeze());
            if (amt.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 自由区：available + frozen，Redis 价格
        List<FreeTradeSpotUserAssetsEntity> freeTradeSpotAssets = UserAssetsManage.queryFreeTradeSpotAssets(userId);
        for (FreeTradeSpotUserAssetsEntity a : freeTradeSpotAssets) {
            BigDecimal amt = a.getAvailable().add(a.getFrozen());
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 现货网格：available + frozen，Redis 价格
        List<GridSpotUserAssetsEntity> gridSpotAssets = UserAssetsManage.queryGridSpotAssets(userId);
        for (GridSpotUserAssetsEntity a : gridSpotAssets) {
            BigDecimal amt = a.getAvailable().add(a.getFrozen());
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 合约跟单：available，Redis 价格
        List<SwapCopyUserAssetsEntity> swapCopyAssets = UserAssetsManage.querySwapCopyAssets(userId);
        for (SwapCopyUserAssetsEntity a : swapCopyAssets) {
            BigDecimal amt = a.getAvailable();
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 全币种合约：available，Redis 价格
        List<MultiCurrencySwapUserAssetsEntity> multiCurrencySwapAssets = UserAssetsManage.queryMultiCurrencySwapAssets(userId);
        for (MultiCurrencySwapUserAssetsEntity a : multiCurrencySwapAssets) {
            BigDecimal amt = a.getAvailable();
            BigDecimal usdt = amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
            WalletCurrencyDetail d = map.computeIfAbsent(a.getCurrency().toLowerCase(), k -> new WalletCurrencyDetail());
            d.total = d.total.add(amt);
            d.totalUsd = d.totalUsd.add(usdt);
        }

        // 合约 持仓浮动盈亏 累加到map.key.currency="usdt"
        BigDecimal unrealizedProfitOrLoss = SwapIncomeCalculate.unrealizedProfitOrLoss(userId, env);
        Optional.ofNullable(map.get("usdt"))
                .filter(detail -> unrealizedProfitOrLoss.signum() != 0)
                .ifPresent(detail -> {
                    detail.total = detail.total.add(unrealizedProfitOrLoss);
                    detail.totalUsd = detail.totalUsd.add(unrealizedProfitOrLoss);
                });

        // 合约跟单 持仓浮动盈亏 累加到map.key.currency="usdt"
        BigDecimal unrealizedProfitOrLossCopySwap = SwapIncomeCalculate.unrealizedProfitOrLossCopySwap(userId, env);
        Optional.ofNullable(map.get("usdt"))
                .filter(detail -> unrealizedProfitOrLossCopySwap.signum() != 0)
                .ifPresent(detail -> {
                    detail.total = detail.total.add(unrealizedProfitOrLossCopySwap);
                    detail.totalUsd = detail.totalUsd.add(unrealizedProfitOrLossCopySwap);
                });

        // 全币种 持仓浮动盈亏 累加到分母币
        List<MultiCurrencySwapPositionEntity> positions = UserAssetsManage.queryMultiCurrencySwapPositions(userId);
        for (MultiCurrencySwapPositionEntity pos : positions) {
            String currency = pos.getCurrency();
            String symbol = pos.getSymbol().replace(currency, "usdt");
            BigDecimal newPrice = NewestPriceUtil.getSwapIndexPrice(symbol, env);
            BigDecimal costOpen = pos.getCostOpen();
            BigDecimal volume = pos.getVolume();
            int direction = pos.getDirection();
            BigDecimal profit = UtilityTools.calcMultiSwapPnl(direction, newPrice, costOpen, volume);
            // 累加到map.key.currency=分母币
            Optional.ofNullable(map.get(currency))
                    .filter(detail -> profit.signum() != 0)
                    .ifPresent(detail -> {
                        detail.total = detail.total.add(profit);
                        detail.totalUsd = detail.totalUsd.add(profit);
                    });
        }

        // 二次遍历：计算 totalBtc 和 assetRatio
        BigDecimal grandTotal = map.values().stream()
                .map(d -> d.totalUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (WalletCurrencyDetail d : map.values()) {
            d.totalBtc = NewestPriceUtil.toBtcAmount(d.totalUsd, "usdt", env);
            if (grandTotal.compareTo(BigDecimal.ZERO) != 0) {
                d.assetRatio = d.totalUsd.divide(grandTotal, 10, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }

        logger.info("按币种钱包资产 map（size=" + map.size() + "）：" + map);
        return map;
    }

    /**
     * 计算各子账户预期 USDT 资产，与 /user-asset/total 接口 assetList 对应。
     * key = accountType（1~10），value = 预期 USDT 总额。
     * type=4（币本位）和 type=9（AMM）暂无独立数据源，不放入 map，调用方跳过断言。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return 子账户类型 → 预期 USDT 总额
     */
    public static Map<Integer, BigDecimal> getSubAccountExpectedMap(long userId, String env) {
        Map<Integer, BigDecimal> map = new HashMap<>();

        // type=1 现货（含 ET/SCS 奖励）
        map.put(1, SpotIncomeCalculate.getCurrentSpotAccount(userId, env));

        // type=2 C2C（OTC/P2P）：available + cashDeposit + frozen，Redis 价格
        BigDecimal otcTotal = BigDecimal.ZERO;
        List<OtcUserAssetsEntity> otcAssets = UserAssetsManage.queryOtcAssets(userId);
        for (OtcUserAssetsEntity a : otcAssets) {
            BigDecimal amt = a.getAvailable().add(a.getCashDeposit()).add(a.getFrozen());
            otcTotal = otcTotal.add(amt.multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        map.put(2, otcTotal);

        // type=3 U 本位合约（含持仓浮盈）
        BigDecimal currentSwapAccount = SwapIncomeCalculate.getCurrentSwapAccount(userId, env);
        BigDecimal unrealizedProfitOrLoss = SwapIncomeCalculate.unrealizedProfitOrLoss(userId, env);
        BigDecimal swapAccountTotal = currentSwapAccount.add(unrealizedProfitOrLoss);
        map.put(3, swapAccountTotal);

        // type=4 币本位账户：暂无独立数据源，跳过
        // type=5 超级合约（仅账户余额，含持仓浮盈）
        BigDecimal multiCurrencySwapAmount = MultiCurrencySwapIncomeCalculate.getCurrentMultiCurrencySwapTotal(userId, env);
        map.put(5, multiCurrencySwapAmount);

        // type=6 理财账户（活期余额 + 定期持仓）
        BigDecimal financeTotal = BigDecimal.ZERO;
        List<FinanceAccountEntity> financeAccounts = UserAssetsManage.queryFinanceAccounts(userId);
        for (FinanceAccountEntity a : financeAccounts) {
            financeTotal = financeTotal.add(a.getBalance().multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        List<FinanceDepositEntity> activeFinanceDeposits = UserAssetsManage.queryActiveFinanceDeposits(userId);
        for (FinanceDepositEntity a : activeFinanceDeposits) {
            financeTotal = financeTotal.add(a.getAmount().multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        map.put(6, financeTotal);

        // type=7 自由区账户：available + frozen，大数据价格
        BigDecimal freeTotal = BigDecimal.ZERO;
        List<FreeTradeSpotUserAssetsEntity> freeTradeSpotAssets = UserAssetsManage.queryFreeTradeSpotAssets(userId);
        for (FreeTradeSpotUserAssetsEntity a : freeTradeSpotAssets) {
            freeTotal = freeTotal.add(a.getAvailable().add(a.getFrozen())
                    .multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        map.put(7, freeTotal);

        // type=8 现货网格账户：available + frozen，Redis 价格
        BigDecimal gridTotal = BigDecimal.ZERO;
        List<GridSpotUserAssetsEntity> gridSpotAssets = UserAssetsManage.queryGridSpotAssets(userId);
        for (GridSpotUserAssetsEntity a : gridSpotAssets) {
            gridTotal = gridTotal.add(a.getAvailable().add(a.getFrozen())
                    .multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        map.put(8, gridTotal);

        // type=9 AMM 账户：暂无数据源，跳过
        // type=10 合约跟单账户：available，Redis 价格
        BigDecimal copyTotal = BigDecimal.ZERO;
        List<SwapCopyUserAssetsEntity> swapCopyAssets = UserAssetsManage.querySwapCopyAssets(userId);
        for (SwapCopyUserAssetsEntity a : swapCopyAssets) {
            copyTotal = copyTotal.add(a.getAvailable()
                    .multiply(NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env)));
        }
        BigDecimal unrealizedProfit = SwapIncomeCalculate.unrealizedProfitOrLossCopySwap(userId, env);
        copyTotal = copyTotal.add(unrealizedProfit);
        map.put(10, copyTotal);

        logger.info("子账户预期资产 map：" + map);
        return map;
    }

    /**
     * 查询全币种合约持仓，按 Redis swap:new:price 计算持仓盈亏，币种→USDT 汇率取 swap:index:price。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev），用于 Redis 价格查询
     * @return 全币种合约未实现盈亏 USDT 总额
     */
    public static BigDecimal getMultiCurrencySwapPositionAmount(Long userId, String env) {
        BigDecimal total = BigDecimal.ZERO;
        List<MultiCurrencySwapPositionEntity> multiCurrencySwapPositions = UserAssetsManage.queryMultiCurrencySwapPositions(userId);
        for (MultiCurrencySwapPositionEntity item : multiCurrencySwapPositions) {
            String currency = item.getCurrency();
            BigDecimal newPrice = NewestPriceUtil.newPriceOfSwap(item.getSymbol().replace(currency, "usdt"), env);
            BigDecimal costOpen = item.getCostOpen();
            int direction = item.getDirection();
            BigDecimal volume = item.getVolume();
            if (costOpen.compareTo(BigDecimal.ZERO) == 0) continue;
            if (direction != 1 && direction != 2) {
                logger.warn("multi_currency_swap_position direction 错误：" + direction);
                continue;
            }
            BigDecimal profit = UtilityTools.calcMultiSwapPnl(direction, newPrice, costOpen, volume);
            BigDecimal currencyUsdtPrice = NewestPriceUtil.getSwapIndexPrice(currency + "_usdt", env);
            total = total.add(profit.multiply(currencyUsdtPrice));
        }
        logger.warn("全币种合约未实现盈亏 USDT：" + total);
        return total;
    }

    public static class WalletCurrencyDetail {
        public BigDecimal total = BigDecimal.ZERO; // 原始数量（币单位汇总）
        public BigDecimal totalUsd = BigDecimal.ZERO; // USDT 估值
        public BigDecimal totalBtc = BigDecimal.ZERO; // BTC 估值（post-loop 计算）
        public BigDecimal assetRatio = BigDecimal.ZERO; // 占总资产百分比（post-loop 计算）
    }
}
