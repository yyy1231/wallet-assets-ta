package com.novax.mapper;

import com.novax.entity.mysql.SwapFrontUserLeverageEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SwapFrontUserLeverageEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapFrontUserLeverageEntity record);

    SwapFrontUserLeverageEntity selectByPrimaryKey(Long id);

    List<SwapFrontUserLeverageEntity> selectAll();

    int updateByPrimaryKey(SwapFrontUserLeverageEntity record);

    SwapFrontUserLeverageEntity selectByUserAndSymbol(@Param("userId") Long userId, @Param("symbol") String symbol);
}