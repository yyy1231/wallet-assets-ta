package com.novax.mapper;

import com.novax.entity.mysql.InternalTransferOrderEntity;

import java.util.List;

public interface InternalTransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(InternalTransferOrderEntity record);

    InternalTransferOrderEntity selectByPrimaryKey(Long id);

    List<InternalTransferOrderEntity> selectAll();

    int updateByPrimaryKey(InternalTransferOrderEntity record);
}