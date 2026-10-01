package com.novax.mapper;

import com.novax.entity.mysql.UserCurrencyOrderQueryEntity;

import java.util.List;

public interface UserCurrencyOrderQueryEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserCurrencyOrderQueryEntity record);

    UserCurrencyOrderQueryEntity selectByPrimaryKey(Long id);

    List<UserCurrencyOrderQueryEntity> selectAll();

    int updateByPrimaryKey(UserCurrencyOrderQueryEntity record);
}