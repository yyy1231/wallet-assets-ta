package com.novax.mapper;

import com.novax.entity.mysql.TaskEtRewardsEntity;

import java.util.List;

public interface TaskEtRewardsMapper {

    List<TaskEtRewardsEntity> selectByUserId(long userId);
}
