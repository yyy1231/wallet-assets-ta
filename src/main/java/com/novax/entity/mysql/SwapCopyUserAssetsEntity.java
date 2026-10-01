package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwapCopyUserAssetsEntity {
    private Long id;

    private Long userId;

    private Long projectId;

    private Long projectMemberId;

    private Byte type;

    private String symbol;

    private String currency;

    private BigDecimal available;

    private BigDecimal frozen;

    private Date createTime;

    private Date updateTime;
}
