package com.novax.testcase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多用户测试配置工具，从 testUsers.properties 读取用户别名与用户组，
 * 供 DataProvider 展开多用户行，以及测试方法内解析 userId。
 */
public class TestUserConfig {

    private static final Properties props = new Properties();
    /** headers 列在 DataProvider 行数组中的下标（tcNum/tcName/tcDescp/reqAddress/headers/params/expect） */
    private static final int HEADERS_COL = 4;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("userId=([^,}]+)");

    static {
        try (InputStream is = TestUserConfig.class.getResourceAsStream("/testUsers.properties")) {
            if (is == null) throw new IllegalStateException("未找到 testUsers.properties");
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("加载 testUsers.properties 失败", e);
        }
    }

    /**
     * DataProvider 行展开，headers 中 userId 字段支持三种写法：
     * <ul>
     *   <li>{@code userId=12345678}       → 1 行，原样保留（向下兼容）</li>
     *   <li>{@code userId=trader_a}       → 1 行，alias 解析为真实 ID</li>
     *   <li>{@code userId=group:swap_all} → N 行，每个组成员各一行</li>
     * </ul>
     */
    public static Object[][] expand(Object[][] rows, String env) {
        List<Object[]> result = new ArrayList<>();
        for (Object[] row : rows) {
            String headers = (String) row[HEADERS_COL];
            Matcher m = USER_ID_PATTERN.matcher(headers);
            String ref = m.find() ? m.group(1).trim() : "";
            for (long uid : resolveRefs(ref, env)) {
                Object[] newRow = Arrays.copyOf(row, row.length);
                newRow[HEADERS_COL] = headers.replaceAll("userId=[^,}]+", "userId=" + uid);
                result.add(newRow);
            }
        }
        return result.toArray(new Object[0][]);
    }

    /**
     * alias 或原始 ID → userId，供测试方法内调用。
     * 纯数字直接返回，否则从 properties 按 env.alias 查找。
     */
    public static long resolve(String aliasOrId, String env) {
        if (aliasOrId.matches("\\d+")) return Long.parseLong(aliasOrId);
        String val = props.getProperty(env + "." + aliasOrId);
        if (val == null) throw new IllegalArgumentException("未找到用户配置: " + env + "." + aliasOrId);
        return Long.parseLong(val.trim());
    }

    private static List<Long> resolveRefs(String ref, String env) {
        List<Long> ids = new ArrayList<>();
        if (ref.startsWith("group:")) {
            String groupKey = env + ".group." + ref.substring(6);
            String groupVal = props.getProperty(groupKey);
            if (groupVal == null) throw new IllegalArgumentException("未找到用户组: " + groupKey);
            for (String alias : groupVal.split(",")) {
                ids.add(resolve(alias.trim(), env));
            }
        } else if (ref.matches("\\d+")) {
            ids.add(Long.parseLong(ref));
        } else {
            ids.add(resolve(ref, env));
        }
        return ids;
    }
}
