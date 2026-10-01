package com.novax.mapper;

import com.novax.entity.mysql.FinanceDepositEntity;

import java.util.List;

public interface FinanceDepositEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FinanceDepositEntity record);

    FinanceDepositEntity selectByPrimaryKey(Long id);

    List<FinanceDepositEntity> selectAll();

    int updateByPrimaryKey(FinanceDepositEntity record);
}