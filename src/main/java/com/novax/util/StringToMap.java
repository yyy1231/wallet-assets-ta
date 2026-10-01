package com.novax.util;

import org.apache.log4j.Logger;

import java.util.HashMap;

public class StringToMap {
    static Logger logger = Logger.getLogger(StringToMap.class);

    public static HashMap<String, String> stringToMap(String param) {
        // param：ex,{id=2,name=lucy}
        HashMap<String, String> map = new HashMap<>();
        // 去除{}
        String str1 = param.replace("{", "");
        String str2 = str1.replace("}", "");
        String str3 = str2.trim();
        // 根据逗号分隔
        String[] arr = str3.split(",");
        if (arr.length > 0) {
            // put到map
            for (int i = 0; i < arr.length; i++) {
                String element = arr[i].trim();
                String[] arr2 = element.split("=");
                if (arr2.length > 1) {
                    map.put(arr2[0], arr2[1]);
                } else if (arr2.length == 1) {
                    map.put(arr2[0], "");
                }
            }
        } else {
            return new HashMap<>();
        }
        return map;
    }

    public static void main(String[] args) {
        String str = "type=1,page=1,pageSize=10";
        System.out.println(stringToMap(str));
    }
}
