package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SwapFrontUserLeverageEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private String symbol;

    private Integer crossedLeverage;

    private Integer fixedLeverage;

    private Integer fissionLeverage;

    private Date createTime;

    private Date updateTime;
}