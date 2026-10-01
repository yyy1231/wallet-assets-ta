package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyAddressInfoEntity {
    private Long id;

    private String protocol;

    private String address;

    private String privateKey;

    private String publicKey;

    private Byte status;

    private Date createTime;

    private Date updateTime;

    private Long tenantId;
}
