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
public class UserEntity {
    private Long id;

    private Long tenantId;

    private String areaCode;

    private String phone;

    private String email;

    private String password;

    private String salt;

    private String nickname;

    private String headUrl;

    private String trueName;

    private String nationality;

    private Integer idCardType;

    private String idCard;

    private Integer authLevel;

    private Integer isAuth;

    private String assistantCurrency;

    private String language;

    private Integer feeFlag;

    private String inviteCode;

    private Long inviterId;

    private Integer status;

    private Integer level;

    private Date levelUpdateTime;

    private Integer spotDiscount;

    private Integer swapDiscount;

    private Integer swapCurrencyDiscount;

    private String googleKey;

    private Date lastModified;

    private Integer isMerchant;

    private Integer isSwapOpened;

    private Integer isAnswerPassed;

    private Integer swapStatus;

    private Integer isLimitSpot;

    private Integer isLimitWithdrawal;

    private String limitMarket;

    private Integer isDeleted;

    private Integer isStatistics;

    private Integer isWhitelist;

    private Integer type;

    private Integer registerClient;

    private String registerIp;

    private String region;

    private Date createTime;

    private Date updateTime;

}
