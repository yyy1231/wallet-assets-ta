package com.novax.mapper;

import com.novax.entity.mysql.SpotFinanceRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SpotFinanceRecordEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotFinanceRecordEntity record);

    SpotFinanceRecordEntity selectByPrimaryKey(Long id);

    List<SpotFinanceRecordEntity> selectAll();

    int updateByPrimaryKey(SpotFinanceRecordEntity record);

    List<SpotFinanceRecordEntity> selectRechargeAndWithdrawFinanceRecord(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("operation") Integer operation);
}
