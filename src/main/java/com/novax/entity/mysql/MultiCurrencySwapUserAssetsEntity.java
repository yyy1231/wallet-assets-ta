package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultiCurrencySwapUserAssetsEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private String currency;

    private BigDecimal available;

    private BigDecimal frozen;

    private Date createTime;

    private Date updateTime;
}
