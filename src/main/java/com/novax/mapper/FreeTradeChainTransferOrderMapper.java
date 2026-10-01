package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeChainTransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自由区链上充值/提现订单查询 Mapper
 */
public interface FreeTradeChainTransferOrderMapper {

    /** 自由区充值：type IN (1,11)，status=2 */
    List<FreeTradeChainTransferOrderEntity> selectChargeOrders(@Param("userId") long userId,
                                                               @Param("updateTime") String updateTime);

    /** 自由区提现：type IN (2,12)，status=2 */
    List<FreeTradeChainTransferOrderEntity> selectWithdrawOrders(@Param("userId") long userId,
                                                                 @Param("updateTime") String updateTime);
}
