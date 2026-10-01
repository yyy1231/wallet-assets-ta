package com.novax.mapper;

import com.novax.entity.mysql.OtcUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OtcUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(OtcUserAssetsEntity record);

    OtcUserAssetsEntity selectByPrimaryKey(Long id);

    List<OtcUserAssetsEntity> selectAll();

    int updateByPrimaryKey(OtcUserAssetsEntity record);

    List<OtcUserAssetsEntity> selectByUserId(@Param("userId") long userId);
}