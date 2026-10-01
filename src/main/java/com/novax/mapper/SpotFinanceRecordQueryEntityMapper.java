package com.novax.mapper;

import com.novax.entity.mysql.SpotFinanceRecordQueryEntity;

import java.util.List;

public interface SpotFinanceRecordQueryEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotFinanceRecordQueryEntity record);

    SpotFinanceRecordQueryEntity selectByPrimaryKey(Long id);

    List<SpotFinanceRecordQueryEntity> selectAll();

    int updateByPrimaryKey(SpotFinanceRecordQueryEntity record);
}