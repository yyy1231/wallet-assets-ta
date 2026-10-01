package com.novax.mapper;

import com.novax.entity.mysql.MultiCurrencySwapPositionEntity;

import java.util.List;

public interface MultiCurrencySwapPositionEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(MultiCurrencySwapPositionEntity record);

    MultiCurrencySwapPositionEntity selectByPrimaryKey(Long id);

    List<MultiCurrencySwapPositionEntity> selectAll();

    int updateByPrimaryKey(MultiCurrencySwapPositionEntity record);
}