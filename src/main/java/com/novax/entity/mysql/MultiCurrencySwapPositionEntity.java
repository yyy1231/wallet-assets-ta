package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultiCurrencySwapPositionEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Integer mode;

    private String symbol;

    private String currency;

    private BigDecimal costOpen;

    private BigDecimal volume;

    private BigDecimal frozen;

    private BigDecimal margin;

    private BigDecimal addMargin;

    private Integer leverage;

    private Integer direction;

    private Date createTime;

    private Date updateTime;
}
