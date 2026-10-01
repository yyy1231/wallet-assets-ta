package com.novax.testcase;

import com.novax.entity.mysql.UserEntity;
import com.novax.mapper.UserEntityMapper;
import com.novax.util.MyBatisUtil;
import org.apache.log4j.Logger;

public class UserInfo {
    static Logger logger = Logger.getLogger(UserInfo.class);

    // 通过邮箱查userId
    public static UserEntity queryByEmail(String email) {
        return MyBatisUtil.execute(UserEntityMapper.class, m -> m.selectByEmail(email));
    }

    public static UserEntity queryById(Long userId) {
        return MyBatisUtil.execute(UserEntityMapper.class, m -> m.selectByPrimaryKey(userId));
    }
}
