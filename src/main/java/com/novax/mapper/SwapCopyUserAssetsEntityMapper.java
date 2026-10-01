package com.novax.mapper;

import com.novax.entity.mysql.SwapCopyUserAssetsEntity;

import java.util.List;

public interface SwapCopyUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapCopyUserAssetsEntity record);

    SwapCopyUserAssetsEntity selectByPrimaryKey(Long id);

    List<SwapCopyUserAssetsEntity> selectAll();

    int updateByPrimaryKey(SwapCopyUserAssetsEntity record);
}