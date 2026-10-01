package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeChainTransferOrderEntity;

import java.util.List;

public interface FreeTradeChainTransferOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FreeTradeChainTransferOrderEntity record);

    FreeTradeChainTransferOrderEntity selectByPrimaryKey(Long id);

    List<FreeTradeChainTransferOrderEntity> selectAll();

    int updateByPrimaryKey(FreeTradeChainTransferOrderEntity record);
}