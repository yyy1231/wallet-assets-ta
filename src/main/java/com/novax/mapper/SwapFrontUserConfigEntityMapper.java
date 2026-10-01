package com.novax.mapper;

import com.novax.entity.mysql.SwapFrontUserConfigEntity;

import java.util.List;

public interface SwapFrontUserConfigEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapFrontUserConfigEntity record);

    SwapFrontUserConfigEntity selectByPrimaryKey(Long id);

    List<SwapFrontUserConfigEntity> selectAll();

    int updateByPrimaryKey(SwapFrontUserConfigEntity record);

    SwapFrontUserConfigEntity selectByUserId(Long userId);
}