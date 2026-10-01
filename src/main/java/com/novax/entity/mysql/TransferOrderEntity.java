package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferOrderEntity {
    private Long id;

    private Long userId;

    private String currency;

    private String symbol;

    private BigDecimal amount;

    private BigDecimal newPrice;

    private Byte current;

    private Long currentSubAccountId;

    private Byte target;

    private Long targetSubAccountId;

    private Integer type;

    private Integer status;

    private String rejectReason;

    private Integer isException;

    private String exceptionReason;

    private Long updater;

    private Date createTime;

    private Date updateTime;
}
