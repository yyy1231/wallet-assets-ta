package com.novax.mapper;

import com.novax.entity.mysql.SwapMarginEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SwapMarginEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapMarginEntity record);

    SwapMarginEntity selectByPrimaryKey(Long id);

    List<SwapMarginEntity> selectAll();

    int updateByPrimaryKey(SwapMarginEntity record);
    SwapMarginEntity selectTier(@Param("symbol") String symbol, @Param("position") long position);
    SwapMarginEntity selectPosition(@Param("symbol") String symbol, @Param("leverage") int leverage);

    List<SwapMarginEntity> selectBySymbol(String symbol);
}