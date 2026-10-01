package com.novax.mapper;

import com.novax.entity.mysql.MultiCurrencySwapUserAssetsEntity;

import java.util.List;

public interface MultiCurrencySwapUserAssetsMapper {

    List<MultiCurrencySwapUserAssetsEntity> selectByUserId(long userId);
}
