package com.novax.mapper;

import com.novax.entity.mysql.GridSpotUserAssetsEntity;

import java.util.List;

public interface GridSpotUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotUserAssetsEntity record);

    GridSpotUserAssetsEntity selectByPrimaryKey(Long id);

    List<GridSpotUserAssetsEntity> selectAll();

    int updateByPrimaryKey(GridSpotUserAssetsEntity record);
}