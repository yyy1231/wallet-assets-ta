package com.novax.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


public class JsonParseUtil {
    /**
     * @param responseJson ,这个变量是拿到响应字符串通过 json 转换 成 json 对象
     * @param jpath,这个     jpath 指的是用户想要查询 json 对象的值的路径写法 jpath 写法举例：
     *                     1) per_page
     *                     2)data[1]/first_name，data 是一个 json 数组，[1]表示索引
     *                     /first_name 表示 data 数组下某一个元素下的 json 对象的名称为
     *                     first_name
     * @return，返回 first_name 这个 json 对象名称对应的值
     */
    public static String getValueByJPath(JSONObject responseJson, String jpath) {
        // jpath   "HttpStatus"  "support/text"  "data[0]/last_name"
        Object obj = responseJson;
        for (String s : jpath.split("/")) { //[data[0],last_name],层层递进遍历
            if (!s.isEmpty()) {
                if (!(s.contains("[") || s.contains("]"))) {
                    obj = ((JSONObject) obj).get(s);
                } else if (s.contains("[") || s.contains("]")) {
                    obj = ((JSONArray) ((JSONObject) obj).get(s.split("\\[")[0]))
                            .get(Integer.parseInt(s.split("\\[")[1].replaceAll("]", "")));
                } else if (s.contains("{") || s.contains("}")) {
                    obj = ((JSONArray) ((JSONObject) obj).get(s.split("\\[")[0]))
                            .get(Integer.parseInt(s.split("\\{")[1].replaceAll("}", "")));
                }
            }
        }
        return obj.toString();
    }
}
