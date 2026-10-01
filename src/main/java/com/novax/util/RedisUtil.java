package com.novax.util;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedisUtil {
    static int connectionTimeout = 2000;
    static int soTimeout = 3000;
    static int maxAttempts = 5;

    private static final Map<String, JedisCluster> JEDIS_MAP = new ConcurrentHashMap<>();


    public static List<RedisNode> getRedisNodes(String env) {
        List<RedisNode> redisNodeList = new ArrayList<>();
        String redisNodes = PropertyUtil.getRedisNodes(env);
        String[] nodes = redisNodes.split(",");
        for (String node : nodes) {
            RedisNode redisNode = new RedisNode();
            String[] nodeStr = node.split(":");
            redisNode.setHost(nodeStr[0]);
            redisNode.setPort(Integer.parseInt(nodeStr[1]));
            redisNodeList.add(redisNode);
        }
        return redisNodeList;
    }

    public static JedisCluster getJedis(String env) {
        return JEDIS_MAP.computeIfAbsent(env, RedisUtil::createJedisCluster);
    }

    private static JedisCluster createJedisCluster(String env) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(100);
        poolConfig.setMinIdle(20);
        poolConfig.setMaxWaitMillis(3000);

        List<RedisNode> redisNodes = getRedisNodes(env);
        Set<HostAndPort> clusterNodes = new HashSet<>();
        for (RedisNode node : redisNodes) {
            clusterNodes.add(new HostAndPort(node.getHost(), node.getPort()));
        }
        return new JedisCluster(clusterNodes, connectionTimeout, soTimeout, maxAttempts, poolConfig);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class RedisNode {
        String host;
        int port;
    }


    public static void main(String[] args) {
        System.out.println(getJedis("test").hget("spot:new:price", "btc_usdt"));
    }
}
