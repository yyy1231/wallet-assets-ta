package com.novax.entity.mysql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwapFrontUserConfigEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Integer mode;

    private Date createTime;

    private Date updateTime;
}