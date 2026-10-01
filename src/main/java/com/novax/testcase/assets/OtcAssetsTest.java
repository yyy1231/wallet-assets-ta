package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.OtcAssetsCalculate;
import com.novax.testcase.TestUserConfig;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class OtcAssetsTest extends BaseTest {
    public static Logger logger = Logger.getLogger(OtcAssetsTest.class);
    @DataProvider(name = "total", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/OtcAssetsTest.xls", 0), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "OTC用户资产列表")
    public void otc_Total_Normal_Test(String tcNum, String tcName, String tcDescp,
                                      String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getOtcHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        JSONObject data = resp.getJSONObject("data");
        BigDecimal convertValue = data.getBigDecimal("convertValue");
        BigDecimal btcValue = data.getBigDecimal("value");
        JSONArray assetList = data.getJSONArray("assetList");
        //  过滤掉totalAmount=0的数据
        assetList.removeIf(obj -> {
            JSONObject json = (JSONObject) obj;
            BigDecimal totalAmount = json.getBigDecimal("totalAmount");
            return totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0;
        });

        BigDecimal expectTotalU = OtcAssetsCalculate.getCurrentOtcAccount(userId, currentEnv);
        BigDecimal expectedBtc = NewestPriceUtil.toBtcAmount(expectTotalU, "usdt", currentEnv);
        Assert.assertTrue(
                convertValue.subtract(expectTotalU).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " OTC总资产 convertValue 计算有误，测试：" + convertValue + "，预期：" + expectTotalU);
        Assert.assertTrue(btcValue.subtract(expectedBtc).abs().compareTo(BTC_TOLERANT) <= 0,
                "用户：" + userId + " OTC data.value(BTC) 偏差超限，测试：" + btcValue + "，预期：" + expectedBtc);

        Map<String, OtcAssetsCalculate.OtcCoinDetail> detailMap =
                OtcAssetsCalculate.getOtcCoinDetailMap(userId, currentEnv);

        for (int i = 0; i < assetList.size(); i++) {
            JSONObject item = assetList.getJSONObject(i);
            String currency = item.getString("currency");
            BigDecimal total = item.getBigDecimal("totalAmount");
            BigDecimal available = item.getBigDecimal("available");
            BigDecimal frozen = item.getBigDecimal("frozen");
            BigDecimal value = item.getBigDecimal("value");
            String unit = item.getString("unit");
            BigDecimal usdtValue = item.getBigDecimal("usdtValue");

            String tag = "用户：" + userId + " 币种[" + currency + "]";

            // 1. currency 非空
            Assert.assertNotNull(currency, tag + " currency 不应为 null");
            Assert.assertFalse(currency.trim().isEmpty(), tag + " currency 不应为空字符串");

            OtcAssetsCalculate.OtcCoinDetail local =
                    detailMap.getOrDefault(currency.toLowerCase(), new OtcAssetsCalculate.OtcCoinDetail());


            Assert.assertTrue(
                    total.subtract(local.totalAmount).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " totalAmount 偏差超限，测试：" + total + "，预期：" + local.totalAmount);
            Assert.assertTrue(
                    available.subtract(local.available).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " available 偏差超限，测试：" + available + "，预期：" + local.available);
            Assert.assertTrue(
                    frozen.subtract(local.totalFrozen).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " frozen 偏差超限，测试：" + frozen + "，预期：" + local.totalFrozen);
            Assert.assertTrue(
                    usdtValue.subtract(local.usdtValue).abs().compareTo(USDT_TOLERANT) <= 0,
                    tag + " usdtValue 偏差超限，测试：" + usdtValue + "，预期：" + local.usdtValue);
            if ("usdt".equalsIgnoreCase(unit)) {
                Assert.assertTrue(
                        value.subtract(local.usdtValue).abs().compareTo(USDT_TOLERANT) <= 0,
                        tag + " value 偏差超限，测试：" + value + "，预期：" + local.usdtValue);
            } else if ("btc".equalsIgnoreCase(unit)) {
                BigDecimal btcPrice = NewestPriceUtil.newPriceOfSymbol("btc_usdt", currentEnv);
                BigDecimal valueInBtc = value.multiply(btcPrice);
                Assert.assertTrue(valueInBtc.subtract(local.usdtValue).abs().compareTo(USDT_TOLERANT) <= 0,
                        tag + " value(BTC→USDT) 偏差超限，测试：" + valueInBtc + "，预期：" + local.usdtValue);
            } else {
                logger.warn(tag + " unit=" + unit + "（未支持），value 断言跳过");
            }
        }
    }
}
