package com.novax.mapper;

import com.novax.entity.mysql.AdminAddressEntity;

import java.util.List;

public interface AdminAddressEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(AdminAddressEntity record);

    AdminAddressEntity selectByPrimaryKey(Long id);

    List<AdminAddressEntity> selectAll();

    int updateByPrimaryKey(AdminAddressEntity record);

    AdminAddressEntity selectByProtocol(String protocol);
}
