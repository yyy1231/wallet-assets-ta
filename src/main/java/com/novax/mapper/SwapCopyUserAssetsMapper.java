package com.novax.mapper;

import com.novax.entity.mysql.SwapCopyUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合约跟单账户资产查询 Mapper
 * 仅统计 available（无 frozen 字段）
 */
public interface SwapCopyUserAssetsMapper {

    List<SwapCopyUserAssetsEntity> selectByUserId(@Param("userId") long userId);
}
