package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreeTradeChainTransferOrderEntity {
    private Long id;

    private Long userId;

    private Long currencyId;

    private String currency;

    private String protocol;

    private String currencyAddress;

    private String fromAddress;

    private String toAddress;

    private String txId;

    private BigDecimal amount;

    private BigDecimal newPrice;

    private BigDecimal fee;

    private BigDecimal gas;

    private BigDecimal total;

    private BigDecimal usdtTotal;

    private Integer type;

    private Integer status;

    private Long nonce;

    private Integer isException;

    private BigDecimal inflowMin;

    private Integer confirmationAmount;

    private Date blockTime;

    private String rejectReason;

    private String exceptionReason;

    private Date createTime;

    private Long updater;

    private String updaterName;

    private Date updateTime;

    private Long tokenId;

    private Byte isCollect;

    private String latestNotes;
}
