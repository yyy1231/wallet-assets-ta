package com.novax.mapper;

import com.novax.entity.mysql.CurrencyAddressInfoEntity;

import java.util.List;

public interface CurrencyAddressInfoEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CurrencyAddressInfoEntity record);

    CurrencyAddressInfoEntity selectByPrimaryKey(Long id);

    List<CurrencyAddressInfoEntity> selectAll();

    int updateByPrimaryKey(CurrencyAddressInfoEntity record);
}
