package com.novax.mapper;

import com.novax.entity.mysql.UserCurrencyOrderQueryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用于充值/提现金额统计查询，区别于 UserCurrencyOrderEntityMapper（面向实体的 CRUD）
 */
public interface UserCurrencyOrderQueryMapper {

    /** 充值：type IN (1,11) 链上充值 */
    List<UserCurrencyOrderQueryEntity> selectChargeOrders(@Param("userId") long userId,
                                                          @Param("updateTime") String updateTime);

    /** 提现：type IN (2,12) 链上提现 */
    List<UserCurrencyOrderQueryEntity> selectWithdrawOrders(@Param("userId") long userId,
                                                            @Param("updateTime") String updateTime);
}
