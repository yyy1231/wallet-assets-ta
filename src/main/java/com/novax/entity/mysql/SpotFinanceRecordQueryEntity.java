package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotFinanceRecordQueryEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long orderId;

    private String symbol;

    private String currency;

    private BigDecimal price;

    private Integer operation;

    private BigDecimal amount;

    private Integer status;

    private Date createTime;

    private Long inviteeId;
}
