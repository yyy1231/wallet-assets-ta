package com.novax.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;

public class JsonStringToMap {
    public static HashMap<String, Object> jsonStringToMap(String param) {
        JSONObject jsonObject = JSON.parseObject(param);
        return new HashMap<>(jsonObject);
    }
}
