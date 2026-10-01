package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskEtRewardsEntity {
    private Long id;

    private Long userId;

    private Long version;

    private BigDecimal totalFreeze;

    private BigDecimal unfreeze;

    private String currency;

    private Date createTime;

    private Long updater;

    private Date updateTime;

    private Long creator;
}
