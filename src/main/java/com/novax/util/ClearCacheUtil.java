package com.novax.util;


import redis.clients.jedis.JedisCluster;

public class ClearCacheUtil {

    public static void clearCache(String key, String env) {
        RedisUtil.getJedis(env).del(key);
    }

    public static void deleteHashValue(String key, String field, String env) {
        JedisCluster jedis = RedisUtil.getJedis(env);
        jedis.hdel(key, field);
    }
}
