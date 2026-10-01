package com.novax.mapper;

import com.novax.entity.mysql.MultiCurrencySwapPositionEntity;

import java.util.List;

public interface MultiCurrencySwapPositionMapper {

    List<MultiCurrencySwapPositionEntity> selectByUserId(long userId);
}
