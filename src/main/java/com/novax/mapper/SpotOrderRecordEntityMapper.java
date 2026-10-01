package com.novax.mapper;

import com.novax.entity.mysql.SpotOrderRecordEntity;

import java.util.List;

public interface SpotOrderRecordEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotOrderRecordEntity record);

    SpotOrderRecordEntity selectByPrimaryKey(Long id);

    List<SpotOrderRecordEntity> selectAll();

    int updateByPrimaryKey(SpotOrderRecordEntity record);
}