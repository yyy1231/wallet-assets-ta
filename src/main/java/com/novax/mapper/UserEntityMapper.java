package com.novax.mapper;

import com.novax.entity.mysql.UserEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface UserEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserEntity record);

    UserEntity selectByPrimaryKey(Long id);

    List<UserEntity> selectAll();

    int updateByPrimaryKey(UserEntity record);

    UserEntity selectByEmail(String email);

    List<UserEntity> selectTestUsers(@Param("email") String email, @Param("from") Date from, @Param("to") Date to);
}
