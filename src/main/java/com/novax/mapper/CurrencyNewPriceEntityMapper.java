package com.novax.mapper;

import com.novax.entity.mysql.CurrencyNewPriceEntity;

import java.util.List;

public interface CurrencyNewPriceEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CurrencyNewPriceEntity record);

    CurrencyNewPriceEntity selectByPrimaryKey(Long id);

    List<CurrencyNewPriceEntity> selectAll();

    int updateByPrimaryKey(CurrencyNewPriceEntity record);
}