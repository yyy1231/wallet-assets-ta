package com.novax.mapper;

import com.novax.entity.mysql.SwapTransferOrderEntity;

import java.util.List;

public interface SwapTransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapTransferOrderEntity record);

    SwapTransferOrderEntity selectByPrimaryKey(Long id);

    List<SwapTransferOrderEntity> selectAll();

    int updateByPrimaryKey(SwapTransferOrderEntity record);
}