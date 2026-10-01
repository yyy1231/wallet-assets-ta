package com.novax.util;

import java.util.Random;

public class RandomChar {

    /**
     * java生成随机字母6位数
     *
     * @param length [生成随机数的长度]
     */
    public static String getRandomChar(int length) {
        StringBuilder val = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
            val.append((char) (choice + random.nextInt(26)));
        }
        return val.toString();
    }
}
