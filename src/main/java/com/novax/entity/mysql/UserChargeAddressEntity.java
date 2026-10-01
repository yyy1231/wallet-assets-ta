package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChargeAddressEntity {
    private Long id;

    private Long userId;

    private String currency;

    private String protocol;

    private String address;

    private String note;

    private Boolean activated;

    private Boolean isDeleted;

    private Date createTime;

    private Long tenantId;
}
