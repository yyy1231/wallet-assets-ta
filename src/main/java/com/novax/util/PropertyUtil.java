package com.novax.util;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class PropertyUtil {
    static Logger logger = Logger.getLogger(PropertyUtil.class);

    // 使用 Map 存储多套环境配置：Key 为环境名(test/dev)，Value 为对应的属性对象
    private static final Map<String, Properties> PROP_MAP = new ConcurrentHashMap<>();

    public static void init(String env) {
        PROP_MAP.computeIfAbsent(env, k -> {
            try {
                Properties prop = new Properties();
                String fileName = "application_" + k + ".properties";
                InputStream is = PropertyUtil.class.getClassLoader().getResourceAsStream(fileName);
                if (is == null) {
                    throw new RuntimeException("配置文件不存在: " + fileName);
                }
                prop.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                return prop;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("加载配置文件失败: " + k, e);
            }
        });
    }

    public static String getProperty(String env, String key) {
        init(env);
        return PROP_MAP.get(env).getProperty(key);
    }


    public static String getBigdataHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "warehouse.host");
    }

    public static String getWalletHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "wallet.host");
    }

    public static String getSpotHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "spot.host");
    }

    public static String getSwapHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "swap.host");
    }

    public static String getMultiSwapHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "multi.host");
    }

    public static String getCopySwapHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "copy.host");
    }

    public static String getSpotGridHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "grid.host");
    }

    public static String getFinanceHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "finance.host");
    }

    public static String getOtcHost(String env) {
        return "http://" + PropertyUtil.getProperty(env, "otc.host");
    }

    public static String getRedisNodes(String env) {
        return getProperty(env, "redis.nodes");
    }

    public static String getMongoHost(String env) {
        return getProperty(env, "mongo.host");
    }

    public static String getMongoPort(String env) {
        return getProperty(env, "mongo.port");
    }

    public static String getMongoUser(String env) {
        return getProperty(env, "mongo.username");
    }

    public static String getMongoPassword(String env) {
        return getProperty(env, "mongo.password");
    }
}
