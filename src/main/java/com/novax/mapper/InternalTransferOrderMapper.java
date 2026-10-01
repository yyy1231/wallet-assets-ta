package com.novax.mapper;

import com.novax.entity.mysql.InternalTransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InternalTransferOrderMapper {

    /** 内部转入（充值方向）：type=1 */
    List<InternalTransferOrderEntity> selectChargeOrders(@Param("userId") long userId,
                                                         @Param("updateTime") String updateTime);

    /** 内部转出（提现方向）：type IN (2,12) */
    List<InternalTransferOrderEntity> selectWithdrawOrders(@Param("userId") long userId,
                                                           @Param("updateTime") String updateTime);
}
