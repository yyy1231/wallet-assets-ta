package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeSpotUserAssetsEntity;

import java.util.List;

public interface FreeTradeSpotUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FreeTradeSpotUserAssetsEntity record);

    FreeTradeSpotUserAssetsEntity selectByPrimaryKey(Long id);

    List<FreeTradeSpotUserAssetsEntity> selectAll();

    int updateByPrimaryKey(FreeTradeSpotUserAssetsEntity record);
}