package com.novax.util;

import lombok.val;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

public class MarkPriceUtil {
    static Logger logger = Logger.getLogger(MarkPriceUtil.class);

    public static BigDecimal markPriceOfSymbol(String symbol, String env) {
        var markPrice = BigDecimal.ZERO;
        val redisValue = RedisUtil.getJedis(env).hget("swap:mark:price", symbol);
        if (null != redisValue) {
            markPrice = new BigDecimal(redisValue);
        }
        logger.info("币对" + symbol + "的标记价：" + markPrice);
        return markPrice;
    }
}
