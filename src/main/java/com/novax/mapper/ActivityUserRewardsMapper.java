package com.novax.mapper;

import com.novax.entity.mysql.ActivityUserRewardsEntity;

import java.util.List;

public interface ActivityUserRewardsMapper {

    List<ActivityUserRewardsEntity> selectByUserId(long userId);
}
