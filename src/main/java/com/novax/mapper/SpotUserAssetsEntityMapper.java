package com.novax.mapper;

import com.novax.entity.mysql.SpotUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SpotUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotUserAssetsEntity record);

    SpotUserAssetsEntity selectByPrimaryKey(Long id);

    List<SpotUserAssetsEntity> selectAll();

    int updateByPrimaryKey(SpotUserAssetsEntity record);

    List<SpotUserAssetsEntity> selectUserAssets(@Param("userId") Long userId);

    SpotUserAssetsEntity selectByUserId(@Param("userId") Long userId, @Param("currency") String currency, @Param("available") BigDecimal amount);

    List<SpotUserAssetsEntity> selectByCurrency(@Param("userId") Long userId, @Param("currency") String currency);


}