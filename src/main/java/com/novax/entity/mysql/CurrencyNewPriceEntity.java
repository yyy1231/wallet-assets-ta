package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyNewPriceEntity {
    private Long id;

    private Integer priceType;

    private String symbol;

    private BigDecimal newPrice;

    private Date createTime;

    private Date updateTime;
}
