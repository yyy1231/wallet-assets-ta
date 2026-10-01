package com.novax.mapper;

import com.novax.entity.mysql.UserChargeAddressEntity;

import java.util.List;

public interface UserChargeAddressEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserChargeAddressEntity record);

    UserChargeAddressEntity selectByPrimaryKey(Long id);

    List<UserChargeAddressEntity> selectAll();

    int updateByPrimaryKey(UserChargeAddressEntity record);

    UserChargeAddressEntity selectByAddress(String address);
}
