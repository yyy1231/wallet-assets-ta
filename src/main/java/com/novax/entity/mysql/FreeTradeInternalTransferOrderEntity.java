package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreeTradeInternalTransferOrderEntity {
    private Long id;

    private Long userId;

    private Long currencyId;

    private Long fromUserId;

    private Integer accountType;

    private String account;

    private Long accountUserId;

    private String currency;

    private String protocol;

    private String currencyAddress;

    private BigDecimal amount;

    private BigDecimal fee;

    private BigDecimal total;

    private BigDecimal newPrice;

    private BigDecimal usdtTotal;

    private Integer type;

    private Integer status;

    private String rejectReason;

    private String exceptionReason;

    private Date createTime;

    private Long updater;

    private String updaterName;

    private Date updateTime;

    private Byte isCollect;

    private String latestNotes;
}
