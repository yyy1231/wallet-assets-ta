package com.novax.mapper;

import com.novax.entity.mysql.UserCurrencyOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserCurrencyOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserCurrencyOrderEntity record);

    UserCurrencyOrderEntity selectByPrimaryKey(Long id);

    List<UserCurrencyOrderEntity> selectAll();

    int updateByPrimaryKey(UserCurrencyOrderEntity record);

    List<UserCurrencyOrderEntity> selectByChain(@Param("userId") Long userId, @Param("currencyChainId") Integer currencyChainId, @Param("protocol") String protocol);

    UserCurrencyOrderEntity selectByTxId(String txId);

    UserCurrencyOrderEntity selectCollectOrder(@Param("protocol") String protocol, @Param("userId") Long userId, @Param("toAddress") String toAddress, @Param("orderId") Long orderId);

    UserCurrencyOrderEntity selectRechargeGasOrder(@Param("protocol") String protocol, @Param("tokenId") Long orderId);
}
