package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalTransferOrderEntity {
    private Long id;

    private Long userId;

    private Long fromUserId;

    private Integer accountType;

    private Integer userType;

    private String account;

    private Long accountUserId;

    private String currency;

    private BigDecimal amount;

    private BigDecimal newPrice;

    private BigDecimal fee;

    private BigDecimal total;

    private Integer type;

    private Integer status;

    private String rejectReason;

    private String exceptionReason;

    private Integer orderType;

    private Integer transferType;

    private Date createTime;

    private Long updater;

    private Date updateTime;

    private Byte isCollect;

    private String latestNotes;
}
