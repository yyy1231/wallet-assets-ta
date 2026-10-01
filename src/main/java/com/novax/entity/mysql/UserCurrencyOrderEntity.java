package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCurrencyOrderEntity {
    private Long id;

    private Long userId;

    private Long currencyChainId;

    private String protocol;

    private String currency;

    private String fromAddress;

    private String toAddress;

    private String txId;

    private BigDecimal amount;

    private BigDecimal newPrice;

    private BigDecimal fee;

    private BigDecimal gas;

    private BigDecimal total;

    private Integer type;

    private Integer status;

    private Long nonce;

    private Integer isException;

    private BigDecimal inflowMin;

    private Integer confirmationAmount;

    private Date blockTime;

    private String rejectReason;

    private String exceptionReason;

    private Integer userType;

    private Byte opType;

    private Date createTime;

    private Long updater;

    private Date updateTime;

    private Long tokenId;

    private Byte isCollect;

    private String latestNotes;
}
