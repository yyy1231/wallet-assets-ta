package com.novax.testcase;

import com.novax.entity.mysql.*;
import com.novax.mapper.*;
import com.novax.util.MyBatisUtil;

import java.util.List;

/**
 * 各账户类型 MySQL 数据查询入口，所有方法均为静态，底层通过 MyBatisUtil 执行。
 */
public class UserAssetsManage {

    /**
     * 查询现货账户资产（spot_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 现货用户资产列表
     */
    public static List<SpotUserAssetsEntity> querySpotAssets(Long userId) {
        return MyBatisUtil.execute(SpotUserAssetsEntityMapper.class, mapper -> mapper.selectUserAssets(userId));
    }

    /**
     * 查询合约账户资产（swap_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 合约用户资产列表
     */
    public static List<SwapUserAssetsEntity> querySwapAssets(long userId) {
        return MyBatisUtil.execute(SwapUserAssetsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询 OTC 账户资产（otc_user_assets 表）。
     *
     * @param userId 用户ID
     * @return OTC 用户资产列表
     */
    public static List<OtcUserAssetsEntity> queryOtcAssets(long userId) {
        return MyBatisUtil.execute(OtcUserAssetsEntityMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询理财账户信息（finance_account 表）。
     *
     * @param userId 用户ID
     * @return 理财账户列表
     */
    public static List<FinanceAccountEntity> queryFinanceAccounts(long userId) {
        return MyBatisUtil.execute(FinanceAccountMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询用户当前持有的活跃理财申购记录（finance_deposit 表）。
     *
     * @param userId 用户ID
     * @return 活跃理财申购记录列表
     */
    public static List<FinanceDepositEntity> queryActiveFinanceDeposits(long userId) {
        return MyBatisUtil.execute(FinanceDepositMapper.class, mapper -> mapper.selectActiveDepositsByUserId(userId));
    }

    /**
     * 查询用户 ET 任务奖励记录（task_et_rewards 表）。
     *
     * @param userId 用户ID
     * @return ET 奖励记录列表
     */
    public static List<TaskEtRewardsEntity> queryEtRewards(long userId) {
        return MyBatisUtil.execute(TaskEtRewardsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询用户 SCS 活动奖励记录（activity_user_rewards 表）。
     *
     * @param userId 用户ID
     * @return SCS 活动奖励记录列表
     */
    public static List<ActivityUserRewardsEntity> queryScsRewards(long userId) {
        return MyBatisUtil.execute(ActivityUserRewardsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询免费交易现货账户资产（free_trade_spot_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 免费交易现货资产列表
     */
    public static List<FreeTradeSpotUserAssetsEntity> queryFreeTradeSpotAssets(long userId) {
        return MyBatisUtil.execute(FreeTradeSpotUserAssetsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询网格策略现货账户资产（grid_spot_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 网格现货资产列表
     */
    public static List<GridSpotUserAssetsEntity> queryGridSpotAssets(long userId) {
        return MyBatisUtil.execute(GridSpotUserAssetsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询合约跟单账户资产（swap_copy_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 合约跟单资产列表
     */
    public static List<SwapCopyUserAssetsEntity> querySwapCopyAssets(long userId) {
        return MyBatisUtil.execute(SwapCopyUserAssetsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询多币种合约账户资产（multi_currency_swap_user_assets 表）。
     *
     * @param userId 用户ID
     * @return 多币种合约资产列表
     */
    public static List<MultiCurrencySwapUserAssetsEntity> queryMultiCurrencySwapAssets(long userId) {
        return MyBatisUtil.execute(MultiCurrencySwapUserAssetsMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询用户法币充值订单（user_currency_order 表，充值方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 法币充值订单列表
     */
    public static List<UserCurrencyOrderQueryEntity> queryUserCurrencyChargeOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(UserCurrencyOrderQueryMapper.class, mapper -> mapper.selectChargeOrders(userId, updateTime));
    }

    /**
     * 查询用户内部划转充值订单（internal_transfer_order 表，充值方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 内部划转充值订单列表
     */
    public static List<InternalTransferOrderEntity> queryInternalTransferOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(InternalTransferOrderMapper.class, mapper -> mapper.selectChargeOrders(userId, updateTime));
    }

    /**
     * 查询免费交易链上充值划转订单（free_trade_chain_transfer_order 表，充值方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 免费交易链上充值划转订单列表
     */
    public static List<FreeTradeChainTransferOrderEntity> queryFreeTradeChainTransferOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(FreeTradeChainTransferOrderMapper.class, m -> m.selectChargeOrders(userId, updateTime));
    }

    /**
     * 查询免费交易内部充值划转订单（free_trade_internal_transfer_order 表，充值方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 免费交易内部充值划转订单列表
     */
    public static List<FreeTradeInternalTransferOrderEntity> queryFreeTradeInternalTransferOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(FreeTradeInternalTransferOrderMapper.class, m -> m.selectChargeOrders(userId, updateTime));
    }

    /**
     * 查询用户法币提现订单（user_currency_order 表，提现方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 法币提现订单列表
     */
    public static List<UserCurrencyOrderQueryEntity> queryUserCurrencyWithdrawOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(UserCurrencyOrderQueryMapper.class, mapper -> mapper.selectWithdrawOrders(userId, updateTime));
    }

    /**
     * 查询用户内部划转提现订单（internal_transfer_order 表，提现方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 内部划转提现订单列表
     */
    public static List<InternalTransferOrderEntity> queryInternalTransferWithdrawOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(InternalTransferOrderMapper.class, mapper -> mapper.selectWithdrawOrders(userId, updateTime));
    }

    /**
     * 查询免费交易链上提现划转订单（free_trade_chain_transfer_order 表，提现方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 免费交易链上提现划转订单列表
     */
    public static List<FreeTradeChainTransferOrderEntity> queryFreeTradeChainWithdrawOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(FreeTradeChainTransferOrderMapper.class, mapper -> mapper.selectWithdrawOrders(userId, updateTime));
    }

    /**
     * 查询免费交易内部提现划转订单（free_trade_internal_transfer_order 表，提现方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 免费交易内部提现划转订单列表
     */
    public static List<FreeTradeInternalTransferOrderEntity> queryFreeTradeInternalWithdrawOrders(long userId, String updateTime) {
        return MyBatisUtil.execute(FreeTradeInternalTransferOrderMapper.class, mapper -> mapper.selectWithdrawOrders(userId, updateTime));
    }

    /**
     * 查询合约全量持仓记录（swap_all_position 表）。
     *
     * @param userId 用户ID
     * @return 合约持仓列表
     */
    public static List<SwapFrontPositionEntity> querySwapPositions(long userId) {
        return MyBatisUtil.execute(SwapFrontPositionEntityMapper.class, mapper -> mapper.selectAllPositionsByUserId(userId));
    }

    /**
     * 查询多币种合约持仓记录（multi_currency_swap_position 表）。
     *
     * @param userId 用户ID
     * @return 多币种合约持仓列表
     */
    public static List<MultiCurrencySwapPositionEntity> queryMultiCurrencySwapPositions(long userId) {
        return MyBatisUtil.execute(MultiCurrencySwapPositionMapper.class, mapper -> mapper.selectByUserId(userId));
    }

    /**
     * 查询现货账户划入记录（spot_transfer_order 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 现货划入订单列表
     */
    public static List<SpotTransferOrderEntity> querySpotTransferIn(long userId, String updateTime) {
        return MyBatisUtil.execute(SpotTransferOrderMapper.class, m -> m.selectSpotTransferIn(userId, updateTime));
    }

    /**
     * 查询现货账户划出记录（spot_transfer_order 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 现货划出订单列表
     */
    public static List<SpotTransferOrderEntity> querySpotTransferOut(long userId, String updateTime) {
        return MyBatisUtil.execute(SpotTransferOrderMapper.class, m -> m.selectSpotTransferOut(userId, updateTime));
    }

    /**
     * 查询跟单交易划入记录（spot_finance_record 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 跟单划入记录列表
     */
    public static List<SpotFinanceRecordQueryEntity> queryCopyTradingTransferIn(long userId, String updateTime) {
        return MyBatisUtil.execute(SpotFinanceRecordQueryMapper.class, m -> m.selectCopyTradingTransferIn(userId, updateTime));
    }

    /**
     * 查询跟单交易划出记录（spot_finance_record 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 跟单划出记录列表
     */
    public static List<SpotFinanceRecordQueryEntity> queryCopyTradingTransferOut(long userId, String updateTime) {
        return MyBatisUtil.execute(SpotFinanceRecordQueryMapper.class, m -> m.selectCopyTradingTransferOut(userId, updateTime));
    }

    /**
     * 查询合约前端持仓快照记录（swap_position 表）。
     *
     * @param userId 用户ID
     * @return 合约前端持仓列表
     */
    public static List<SwapFrontPositionEntity> querySwapFrontPositions(long userId) {
        return MyBatisUtil.execute(SwapFrontPositionEntityMapper.class, m -> m.selectFrontPositionByUserId(userId));
    }

    /**
     * 查询合约账户划入记录（swap_transfer_order 表，转入方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 合约划入订单列表
     */
    public static List<SwapTransferOrderEntity> querySwapTransferIn(long userId, String updateTime) {
        return MyBatisUtil.execute(SwapTransferOrderMapper.class, m -> m.selectSwapTransferIn(userId, updateTime));
    }

    /**
     * 查询合约账户划出记录（swap_transfer_order 表，转出方向）。
     *
     * @param userId     用户ID
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 合约划出订单列表
     */
    public static List<SwapTransferOrderEntity> querySwapTransferOut(long userId, String updateTime) {
        return MyBatisUtil.execute(SwapTransferOrderMapper.class, m -> m.selectSwapTransferOut(userId, updateTime));
    }

    /**
     * 查询现货当日成交订单记录（spot_order_record 表）。
     *
     * @param userId     用户ID
     * @param createTime 日期字符串 yyyy-MM-dd，按创建时间过滤
     * @return 现货当日订单列表
     */
    public static List<SpotOrderRecordEntity> querySpotTodayOrders(long userId, String createTime) {
        return MyBatisUtil.execute(SpotOrderRecordMapper.class, m -> m.selectTodayOrders(userId, createTime));
    }

    /**
     * 按划转类型查询划转订单记录（transfer_order 表）。
     *
     * @param userId     用户ID
     * @param type       划转类型
     * @param updateTime 日期字符串 yyyy-MM-dd，按更新时间过滤
     * @return 指定类型的划转订单列表
     */
    public static List<TransferOrderEntity> queryTransferByType(long userId, int type, String updateTime) {
        return MyBatisUtil.execute(TransferOrderMapper.class, m -> m.selectByUserIdAndType(userId, type, updateTime));
    }

}
