package com.novax.util;


import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableReplace {
    static Logger logger = Logger.getLogger(VariableReplace.class);

    public static String replace(String str, String projectId, String projectMemberId, String positionId) {
        String pattern = "\\{[^}]*}";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        while (m.find()) {
            String g = m.group();
            String target = g.substring(1, g.length() - 1);// 去掉花括号
            if (target.equals("projectId")) {
                str = str.replace(g, projectId);
            }
            if (target.equals("memberId")) {
                str = str.replace(g, projectMemberId);
            }
            if (target.equals("projectMemberId")) {
                str = str.replace(g, projectMemberId);
            }
            if (target.equals("positionId")) {
                str = str.replace(g, positionId);
            }
            if (target.equals("plannedOrderId")) {
                str = str.replace(g, positionId);
            }
        }
        logger.info("str:" + str);
        return str;
    }

    public static String replace(String str, String value) {
        logger.info("replace入参：" + str + " " + value);
        String pattern = "\\{[^}]*}";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        while (m.find()) {
            String g = m.group();
            // 去掉花括号
            str = str.replace(g, value);
        }
        logger.info("str:" + str);
        return str;
    }

    public static String replace(String str, String currency, String memberId) {
        String pattern = "\\{[^}]*}";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        while (m.find()) {
            String g = m.group();
            String target = g.substring(1, g.length() - 1);// 去掉花括号
            if (target.equals("currency")) {
                str = str.replace(g, currency);
            }
            if (target.equals("memberId")) {
                str = str.replace(g, memberId);
            }
        }
        logger.info("str:" + str);
        return str;
    }
}
