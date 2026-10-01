package com.novax.mapper;

import com.novax.entity.mysql.TransferOrderEntity;

import java.util.List;

public interface TransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TransferOrderEntity record);

    TransferOrderEntity selectByPrimaryKey(Long id);

    List<TransferOrderEntity> selectAll();

    int updateByPrimaryKey(TransferOrderEntity record);
}