package com.novax.mapper;

import com.novax.entity.mysql.SpotOrderRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SpotOrderRecordMapper {

    /**
     * 查询今日现货交易记录
     * 字段：symbol, trade_type(1买/2卖), usdt_price, total_currency
     */
    List<SpotOrderRecordEntity> selectTodayOrders(@Param("userId") long userId,
                                                  @Param("createTime") String createTime);
}
