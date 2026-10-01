package com.novax.mapper;

import com.novax.entity.mysql.TaskEtRewardsEntity;

import java.util.List;

public interface TaskEtRewardsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TaskEtRewardsEntity record);

    TaskEtRewardsEntity selectByPrimaryKey(Long id);

    List<TaskEtRewardsEntity> selectAll();

    int updateByPrimaryKey(TaskEtRewardsEntity record);
}