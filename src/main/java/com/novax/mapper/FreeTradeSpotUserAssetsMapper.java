package com.novax.mapper;

import com.novax.entity.mysql.FreeTradeSpotUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自由区现货账户查询 Mapper
 * currency_id 为数字型币种 ID（价格换算需使用自由区专属价格接口）
 */
public interface FreeTradeSpotUserAssetsMapper {

    List<FreeTradeSpotUserAssetsEntity> selectByUserId(@Param("userId") long userId);
}
