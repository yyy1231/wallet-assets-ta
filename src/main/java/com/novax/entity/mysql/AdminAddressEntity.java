package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAddressEntity {
    private Long id;

    private String protocol;

    private Long groupId;

    private Byte isPlatform;

    private String currency;

    private Long chainId;

    private String address;

    private String memo;

    private String remark;

    private Byte status;

    private Byte isDeleted;

    private Long creator;

    private Date createTime;

    private Long updater;

    private Date updateTime;
}
