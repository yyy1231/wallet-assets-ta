package com.novax.util;

import com.alibaba.fastjson.JSONObject;
import com.novax.mapper.CurrencyNewPriceMapper;
import lombok.val;
import org.apache.log4j.Logger;
import redis.clients.jedis.JedisCluster;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NewestPriceUtil {
    static Logger logger = Logger.getLogger(NewestPriceUtil.class);

    // 现货最新价（spot:new:price）
    public static BigDecimal newPriceOfSymbol(String symbol, String env) {
        if ("usdt_usdt".equals(symbol)) return BigDecimal.ONE;
        var newPrice = BigDecimal.ZERO;
        val redisValue = RedisUtil.getJedis(env).hget("spot:new:price", symbol);
        val parsedObj = JSONObject.parseObject(redisValue);
        if (null != parsedObj && null != parsedObj.getBigDecimal("price")) {
            newPrice = parsedObj.getBigDecimal("price");
        }
        logger.info("币对" + symbol + "的现货最新价：" + newPrice);
        return newPrice;
    }

    // 合约最新价（swap:new:price JSON）
    public static BigDecimal newPriceOfSwap(String symbol, String env) {
        var newPrice = BigDecimal.ZERO;
        val redisValue = RedisUtil.getJedis(env).hget("swap:new:price", symbol);
        val parsedObj = JSONObject.parseObject(redisValue);
        if (null != parsedObj && null != parsedObj.getBigDecimal("price")) {
            newPrice = parsedObj.getBigDecimal("price");
        }
        logger.info("币对" + symbol + "的合约最新价：" + newPrice);
        return newPrice;
    }

    // 全币种合约指数价（swap:index:price raw 值）
    public static BigDecimal getSwapIndexPrice(String symbol, String env) {
        try {
            String price = RedisUtil.getJedis(env).hget("swap:index:price", symbol);
            if (symbol.equals("usdt_usdt")) return BigDecimal.ONE;
            if (price == null) return BigDecimal.ZERO;
            return new BigDecimal(price);
        } catch (Exception e) {
            logger.warn("Redis swap:index:price 查询失败 symbol=" + symbol + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // MySQL 大数据价格（by currency，自动拼 _usdt）
    public static BigDecimal getBigDataPrice(String currency) {
        if ("usdt".equals(currency)) return BigDecimal.ONE;
        try {
            BigDecimal price = MyBatisUtil.execute(CurrencyNewPriceMapper.class,
                    m -> m.selectLatestPrice(currency + "_usdt", 1));
            return price != null ? price : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.warn("big data 价格查询失败 currency=" + currency + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // MySQL 大数据价格（by 完整 symbol）
    public static BigDecimal getBigDataPriceBySymbol(String symbol) {
        try {
            BigDecimal price = MyBatisUtil.execute(CurrencyNewPriceMapper.class,
                    m -> m.selectLatestPrice(symbol, 1));
            return price != null ? price : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.warn("big data 价格查询失败 symbol=" + symbol + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // USDT → BTC 换算（MySQL 大数据价格）
    public static BigDecimal toBtcAmount(BigDecimal usdtAmount, String env) {
        if (usdtAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        // BigDecimal btcPrice = getBigDataPrice("btc");
        BigDecimal btcPrice = newPriceOfSymbol("btc_usdt", env);
        if (btcPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return usdtAmount.divide(btcPrice, 20, RoundingMode.HALF_UP);
    }

    // 任意币种 → BTC 换算（Redis 现货价格）
    // currency=btc: 直接返回; currency=usdt: amount/btc价; 其他: amount×币价/btc价
    public static BigDecimal toBtcAmount(BigDecimal amount, String currency, String env) {
        if ("btc".equals(currency)) return amount;
        BigDecimal btcPrice = newPriceOfSymbol("btc_usdt", env);
        if (btcPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if ("usdt".equals(currency)) {
            return amount.divide(btcPrice, 20, RoundingMode.HALF_UP);
        }
        BigDecimal usdtAmount = amount.multiply(newPriceOfSymbol(currency + "_usdt", env));
        return usdtAmount.divide(btcPrice, 20, RoundingMode.HALF_UP);
    }

    // 写入价格到 Redis 有序集合
    public static Long writeSwapPrice(String key, String symbol, Double value, String env) {
        JedisCluster jedis = RedisUtil.getJedis(env);
        return jedis.zadd(key + symbol, System.currentTimeMillis(), String.valueOf(value));
    }

    public static void main(String[] args) {
        // System.out.println(toBtcAmount(BigDecimal.valueOf(1000)));
        // System.out.println(newPriceOfSymbol("aisi_usdt", "test"));
        System.out.println(newPriceOfSwap("btc_usdt", "test"));
    }
}
