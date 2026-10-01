package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotOrderRecordEntity {
    private Long id;

    private Long userId;

    private String region;

    private Long orderId;

    private String symbol;

    private BigDecimal price;

    private BigDecimal tradeNumber;

    private String totalCurrency;

    private BigDecimal total;

    private BigDecimal usdtPrice;

    private Integer tradeType;

    private BigDecimal fee;

    private BigDecimal usdtFee;

    private String feeCurrency;

    private Integer status;

    private Date createTime;

    private Date createTimeKey;
}
