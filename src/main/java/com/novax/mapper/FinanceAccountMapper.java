package com.novax.mapper;

import com.novax.entity.mysql.FinanceAccountEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 理财账户（利息）查询 Mapper
 */
public interface FinanceAccountMapper {

    List<FinanceAccountEntity> selectByUserId(@Param("userId") long userId);
}
