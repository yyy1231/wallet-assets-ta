package com.novax.mapper;

import com.novax.entity.mysql.MultiCurrencySwapUserAssetsEntity;

import java.util.List;

public interface MultiCurrencySwapUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(MultiCurrencySwapUserAssetsEntity record);

    MultiCurrencySwapUserAssetsEntity selectByPrimaryKey(Long id);

    List<MultiCurrencySwapUserAssetsEntity> selectAll();

    int updateByPrimaryKey(MultiCurrencySwapUserAssetsEntity record);
}