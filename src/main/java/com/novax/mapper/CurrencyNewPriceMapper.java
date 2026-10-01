package com.novax.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface CurrencyNewPriceMapper {

    /**
     * price_type: 1=现货  2=合约
     */
    BigDecimal selectLatestPrice(@Param("symbol") String symbol,
                                 @Param("priceType") int priceType);
}
