package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyChainEntity {
    private Long id;

    private String currency;

    private String protocol;

    private Byte currencyType;

    private String chainName;

    private String smartContractAddress;

    private String gasAddress;

    private Integer inflowConfirmNumber;

    private Integer availableConfirmNumber;

    private Integer outflowConfirmNumber;

    private BigDecimal inflowMin;

    private Byte feeType;

    private BigDecimal outflowFee;

    private BigDecimal outflowFeeRate;

    private BigDecimal outflowMin;

    private BigDecimal outflowMax;

    private BigDecimal outflowAutomatic;

    private Integer currencyDigit;

    private String memoRegex;

    private Integer inflowType;

    private Integer outflowType;

    private BigDecimal cautionNumber;

    private Byte status;

    private Date createTime;

    private Date updateTime;
}
