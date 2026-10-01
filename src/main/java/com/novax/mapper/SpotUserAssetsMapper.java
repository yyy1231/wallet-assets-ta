package com.novax.mapper;

import com.novax.entity.mysql.SpotUserAssetsEntity;

import java.util.List;

public interface SpotUserAssetsMapper {

    List<SpotUserAssetsEntity> selectByUserId(long userId);
}
