package com.novax.mapper;

import com.novax.entity.mysql.FinanceDepositEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 理财持仓查询 Mapper
 * state: 1=持有中, 2=到期待赎回
 */
public interface FinanceDepositMapper {

    List<FinanceDepositEntity> selectActiveDepositsByUserId(@Param("userId") long userId);
}
