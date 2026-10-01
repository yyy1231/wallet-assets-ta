package com.novax.mapper;

import com.novax.entity.mysql.FinanceAccountEntity;

import java.util.List;

public interface FinanceAccountEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FinanceAccountEntity record);

    FinanceAccountEntity selectByPrimaryKey(Long id);

    List<FinanceAccountEntity> selectAll();

    int updateByPrimaryKey(FinanceAccountEntity record);
}