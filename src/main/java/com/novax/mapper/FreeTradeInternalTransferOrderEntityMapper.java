package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeInternalTransferOrderEntity;

import java.util.List;

public interface FreeTradeInternalTransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FreeTradeInternalTransferOrderEntity record);

    FreeTradeInternalTransferOrderEntity selectByPrimaryKey(Long id);

    List<FreeTradeInternalTransferOrderEntity> selectAll();

    int updateByPrimaryKey(FreeTradeInternalTransferOrderEntity record);
}