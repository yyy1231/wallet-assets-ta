package com.novax.mapper;

import com.novax.entity.mysql.TransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransferOrderMapper {

    List<TransferOrderEntity> selectByUserIdAndType(
            @Param("userId") long userId,
            @Param("typeIn") int typeIn,
            @Param("updateTime") String updateTime);
}
