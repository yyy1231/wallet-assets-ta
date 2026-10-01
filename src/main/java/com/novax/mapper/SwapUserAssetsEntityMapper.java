package com.novax.mapper;

import com.novax.entity.mysql.SwapUserAssetsEntity;

import java.util.List;

public interface SwapUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapUserAssetsEntity record);

    SwapUserAssetsEntity selectByPrimaryKey(Long id);

    List<SwapUserAssetsEntity> selectAll();

    int updateByPrimaryKey(SwapUserAssetsEntity record);
}