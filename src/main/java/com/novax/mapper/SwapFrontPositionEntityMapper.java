package com.novax.mapper;

import com.novax.entity.mysql.SwapFrontPositionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SwapFrontPositionEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapFrontPositionEntity record);

    SwapFrontPositionEntity selectByPrimaryKey(Long id);

    List<SwapFrontPositionEntity> selectAll();

    int updateByPrimaryKey(SwapFrontPositionEntity record);

    List<SwapFrontPositionEntity> selectFrontPositionByUserId(@Param("userId") long userId);

    List<SwapFrontPositionEntity> selectPositions(@Param("userId") Long userId, @Param("symbol") String symbol, @Param("direction") Integer direction);

    List<SwapFrontPositionEntity> selectCopyPositions(@Param("userId") Long userId, @Param("symbol") String symbol, @Param("direction") Integer direction);

    List<SwapFrontPositionEntity> selectAllPositionsByUserId(@Param("userId") long userId);

}