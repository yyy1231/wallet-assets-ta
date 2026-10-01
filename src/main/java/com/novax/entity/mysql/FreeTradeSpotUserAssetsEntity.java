package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreeTradeSpotUserAssetsEntity {
    private Long id;

    private Long userId;

    private String currency;

    private String protocol;

    private String currencyAddress;

    private Long currencyId;

    private BigDecimal available;

    private BigDecimal frozen;

    private Date createTime;

    private Date updateTime;
}
