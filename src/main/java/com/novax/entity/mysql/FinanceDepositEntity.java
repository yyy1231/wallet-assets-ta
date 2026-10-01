package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceDepositEntity {
    private Long id;

    private Date createTime;

    private Date updateTime;

    private Long userId;

    private Long productId;

    private Long depositPurchaseId;

    private Byte type;

    private Short dayLimit;

    private Byte state;

    private Byte isRenew;

    private String currency;

    private BigDecimal amount;

    private BigDecimal frozen;

    private Date startTime;

    private Date endTime;

    private Date redemptionTime;

    private Date receiveTime;

    private Byte isPreRedemption;
}
