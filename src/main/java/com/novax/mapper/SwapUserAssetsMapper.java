package com.novax.mapper;

import com.novax.entity.mysql.SwapUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * swap_front_user_assets 查询 Mapper
 */
public interface SwapUserAssetsMapper {

    List<SwapUserAssetsEntity> selectByUserId(@Param("userId") long userId);
}
