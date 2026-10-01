package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwapFrontPositionEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Integer mode;

    private String symbol;

    private BigDecimal costOpen;

    private Long volume;

    private Long frozen;

    private BigDecimal margin;

    private BigDecimal addMargin;

    private Integer leverage;

    private BigDecimal contractSize;

    private Integer direction;

    private Byte source;

    private Long sourceId;

    private Date createTime;

    private Date updateTime;
}
