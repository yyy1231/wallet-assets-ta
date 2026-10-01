package com.novax.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class MyBatisUtil {
    private static final Map<String, SqlSessionFactory> FACTORY_MAP = new ConcurrentHashMap<>();
    // 线程局部变量，存储当前线程默认使用的环境名称（test或dev）
    private static final ThreadLocal<String> CURRENT_ENV = new ThreadLocal<>();

    public static void init(String env) {
        if (!FACTORY_MAP.containsKey(env)) {
            try {
                String resource = "mybatis-config-" + env + ".xml";
                InputStream is = Resources.getResourceAsStream(resource);
                FACTORY_MAP.put(env, new SqlSessionFactoryBuilder().build(is));
            } catch (IOException e) {
                throw new RuntimeException("初始化" + env + "数据库失败", e);
            }
        }
    }

    public static void setCurrentEnv(String env) {
        CURRENT_ENV.set(env);
    }

    // 默认获取当前线程绑定的环境（给业务代码用，无需改动已有代码）
    public static SqlSession getSqlSession() {
        String env = CURRENT_ENV.get();
        if (env == null) {
            // 报错提醒：说明当前线程没有通过 setCurrentEnv 设置过环境
            throw new RuntimeException("MyBatis Error: 当前线程未绑定环境，请先调用 setCurrentEnv()");
        }
        return getSqlSession(env);
    }

    // 指定环境获取（给双库断言用）
    public static SqlSession getSqlSession(String env) {
        SqlSessionFactory factory = FACTORY_MAP.get(env);
        if (factory == null) {
            init(env); // 自动兜底初始化
            factory = FACTORY_MAP.get(env);
        }
        return factory.openSession();
    }

    // 有返回值的（查询） 自动关闭收回连接
    public static <T, R> R execute(String env, Class<T> mapperClass, Function<T, R> action) {
        try (SqlSession session = getSqlSession(env)) {
            T mapper = session.getMapper(mapperClass);
            return action.apply(mapper);
        }
    }

    // 不带 env 参数的重载版本，内部自动获取当前线程的环境
    public static <T, R> R execute(Class<T> mapperClass, Function<T, R> action) {
        String env = CURRENT_ENV.get(); // 获取当前线程绑定的环境
        if (env == null) throw new RuntimeException("当前线程未绑定环境");
        return execute(env, mapperClass, action);
    }


    // 无返回值的（增删改）
    public static <T> void executeVoid(String env, Class<T> mapperClass, Consumer<T> action) {
        try (SqlSession session = getSqlSession(env)) {
            T mapper = session.getMapper(mapperClass);
            action.accept(mapper);
            session.commit(); // 自动提交事务
        }
    }

    public static <T> void executeVoid(Class<T> mapperClass, Consumer<T> action) {
        String env = CURRENT_ENV.get(); // 获取当前线程绑定的环境
        if (env == null) throw new RuntimeException("当前线程未绑定环境");
        executeVoid(env, mapperClass, action);
    }

}
