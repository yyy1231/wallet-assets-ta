package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyChainConfigEntity {
    private Long id;

    private String protocol;

    private String imgs;

    private String mainCurrency;

    private String walletServiceIp;

    private String walletServicePort;

    private String walletServiceName;

    private String walletServicePassword;

    private String walletServiceAccount;

    private String apiKey;

    private BigDecimal execCollectParam;

    private Byte status;

    private Integer inflowType;

    private Integer outflowType;

    private Long chainId;

    private Byte authStatus;

    private Integer sort;

    private String contractAddressUrl;

    private String addressUrl;

    private String transactionUrl;

    private Date createTime;

    private Date updateTime;
}
