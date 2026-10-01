package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceAccountEntity {
    private Long id;

    private Date createTime;

    private Date updateTime;

    private Long userId;

    private String currency;

    private BigDecimal balance;

    private BigDecimal flexibleBalance;

    private BigDecimal frozenBalance;
}
