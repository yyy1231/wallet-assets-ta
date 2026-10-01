package com.novax.mapper;

import com.novax.entity.mysql.GridSpotUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 网格账户资产查询 Mapper
 */
public interface GridSpotUserAssetsMapper {

    List<GridSpotUserAssetsEntity> selectByUserId(@Param("userId") long userId);
}
