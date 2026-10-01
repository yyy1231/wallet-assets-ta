package com.novax.mapper;

import com.novax.entity.mysql.SwapFrontUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SwapFrontUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SwapFrontUserAssetsEntity record);

    SwapFrontUserAssetsEntity selectByPrimaryKey(Long id);

    List<SwapFrontUserAssetsEntity> selectAll(Long userId);

    int updateByPrimaryKey(SwapFrontUserAssetsEntity record);
    SwapFrontUserAssetsEntity selectByUserId(@Param("userId") Long userId, @Param("currency") String currency);

    SwapFrontUserAssetsEntity selectBySymbol(@Param("userId") Long userId, @Param("currency") String currency, @Param("symbol") String symbol);

    SwapFrontUserAssetsEntity selectByMode(@Param("userId") Long userId, @Param("currency") String currency, @Param("symbol") String tradeSymbol);
}