package com.novax.mapper;

import com.novax.entity.mysql.CurrencyChainConfigEntity;

import java.util.List;

public interface CurrencyChainConfigEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CurrencyChainConfigEntity record);

    CurrencyChainConfigEntity selectByPrimaryKey(Long id);

    List<CurrencyChainConfigEntity> selectAll();

    int updateByPrimaryKey(CurrencyChainConfigEntity record);
}
