package com.novax.mapper;

import com.novax.entity.mysql.CurrencyChainEntity;

import java.util.List;

public interface CurrencyChainEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CurrencyChainEntity record);

    CurrencyChainEntity selectByPrimaryKey(Long id);

    List<CurrencyChainEntity> selectAll();

    int updateByPrimaryKey(CurrencyChainEntity record);

    CurrencyChainEntity selectByChain(Long chainId);
}
