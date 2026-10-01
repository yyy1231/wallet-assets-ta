package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityUserRewardsEntity {
    private Long id;

    private Long activityId;

    private Long userId;

    private BigDecimal totalFreeze;

    private BigDecimal unfreeze;

    private String currency;

    private Date createTime;

    private Date updateTime;
}
