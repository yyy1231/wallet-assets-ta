package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeInternalTransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自由区内部转账订单查询 Mapper
 * 注意：amount 已折算为 USDT，直接使用 usdt_total 字段，无需再乘价格
 */
public interface FreeTradeInternalTransferOrderMapper {

    /** 自由区内部充值：type IN (1,11)，status=2 */
    List<FreeTradeInternalTransferOrderEntity> selectChargeOrders(@Param("userId") long userId,
                                                                   @Param("updateTime") String updateTime);

    /** 自由区内部提现：type IN (2,12)，status=2 */
    List<FreeTradeInternalTransferOrderEntity> selectWithdrawOrders(@Param("userId") long userId,
                                                                     @Param("updateTime") String updateTime);
}
