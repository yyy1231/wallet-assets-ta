package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.TestUserConfig;
import com.novax.testcase.TodayTenMinAssetCalculate;
import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.PropertyUtil;
import com.novax.util.StringToMap;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;

public class TodayTenMinAssetTest extends BaseTest {
    public static Logger logger = Logger.getLogger(TodayTenMinAssetTest.class);
    @DataProvider(name = "datas", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/TodayTenMinAssetTest.xls", 0), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "今日10分钟资产")
    public void todayTenMinAsset_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getBigdataHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        JSONArray trendData = resp.getJSONArray("data");

        // 业务断言：最新10分钟快照 ≈ 当前总资产（< 10 USDT 容差）
        BigDecimal[] localTotal = TodayTenMinAssetCalculate.currentWalletAssetsTotal(userId, currentEnv);
        BigDecimal localUsdt = localTotal[0];

        if (trendData != null && !trendData.isEmpty()) {
            // 取最后一个时间点（时间正序时为最新；若接口返回倒序则改为 index 0）
            // JSONObject latestPoint = trendData.getJSONObject(trendData.size() - 1);
            JSONObject latestPoint = trendData.getJSONObject(0);
            BigDecimal usdtPrice = latestPoint.getBigDecimal("usdtPrice");
            Assert.assertTrue(
                    usdtPrice.subtract(localUsdt).abs().compareTo(USDT_TOLERANT) <= 0,
                    "用户：" + userId + " 10分钟快照与当前总资产偏差超限，测试：" + usdtPrice + "，预期：" + localUsdt);
        }
    }
}
