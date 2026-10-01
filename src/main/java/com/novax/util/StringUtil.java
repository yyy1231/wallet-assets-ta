package com.novax.util;

public class StringUtil {
    // 字符串转小写 除首字母
    public static String toLowerCaseExceptFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // 截取第0位 + 截取第1位之后的所有字符转小写
        return str.substring(0, 1) + str.substring(1).toLowerCase();
    }
}
