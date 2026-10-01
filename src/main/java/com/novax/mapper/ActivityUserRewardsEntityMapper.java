package com.novax.mapper;

import com.novax.entity.mysql.ActivityUserRewardsEntity;

import java.util.List;

public interface ActivityUserRewardsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ActivityUserRewardsEntity record);

    ActivityUserRewardsEntity selectByPrimaryKey(Long id);

    List<ActivityUserRewardsEntity> selectAll();

    int updateByPrimaryKey(ActivityUserRewardsEntity record);
}