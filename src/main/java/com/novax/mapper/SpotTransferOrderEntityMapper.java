package com.novax.mapper;

import com.novax.entity.mysql.SpotTransferOrderEntity;

import java.util.List;

public interface SpotTransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotTransferOrderEntity record);

    SpotTransferOrderEntity selectByPrimaryKey(Long id);

    List<SpotTransferOrderEntity> selectAll();

    int updateByPrimaryKey(SpotTransferOrderEntity record);
}