package com.novax.mapper;

import com.novax.entity.mysql.SpotTransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 现货账户维度的划转查询（转入/转出）
 */
public interface SpotTransferOrderMapper {

    /** 转入现货：type IN (2,4,6,8,10,12,14) */
    List<SpotTransferOrderEntity> selectSpotTransferIn(@Param("userId") long userId,
                                                       @Param("updateTime") String updateTime);

    /** 转出现货：type IN (1,3,5,7,9,11,13) */
    List<SpotTransferOrderEntity> selectSpotTransferOut(@Param("userId") long userId,
                                                        @Param("updateTime") String updateTime);
}
