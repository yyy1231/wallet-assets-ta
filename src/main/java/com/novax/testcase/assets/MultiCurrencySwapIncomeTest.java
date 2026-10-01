package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.MultiCurrencySwapIncomeCalculate;
import com.novax.testcase.TestUserConfig;
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

public class MultiCurrencySwapIncomeTest extends BaseTest {
    public static Logger logger = Logger.getLogger(MultiCurrencySwapIncomeTest.class);
    @DataProvider(name = "total", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/MultiCurrencySwapIncomeTest.xls", 0), currentEnv);
    }

    @DataProvider(name = "profit", parallel = true)
    public Object[][] getProvider2() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/MultiCurrencySwapIncomeTest.xls", 1), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "全币种合约总资产估值")
    public void multiCurrencySwap_Total_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getMultiSwapHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:"    + reqHeaders);
        logger.info(tcNum + "-入参:"       + null);
        logger.info(tcNum + "-响应数据:"   + resp);
        logger.info(tcNum + "-预期结果:"   + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 业务断言：全币种合约全部资产（账户余额 + 未实现盈亏）≈ 当前实时计算值（< 10 USDT 容差）
        JSONObject data = resp.getJSONObject("data");
        BigDecimal totalUsdt = data.getBigDecimal("valuationCurrency");
        BigDecimal expectTotalU = MultiCurrencySwapIncomeCalculate.getCurrentMultiCurrencySwapTotal(userId, currentEnv);
        Assert.assertTrue(
                totalUsdt.subtract(expectTotalU).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " 全币种合约总资产计算有误，测试：" + totalUsdt + "，预期：" + expectTotalU);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "profit", description = "全币种周期盈亏/盈亏率")
    public void multiCurrencySwapIncome_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getMultiSwapHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 业务断言：遍历三个周期（type=1/7/30），逐一断言
        JSONArray pnlData = resp.getJSONArray("data");
        Assert.assertNotNull(pnlData, "用户：" + userId + " data 不应为 null");
        Assert.assertFalse(pnlData.isEmpty(), "用户：" + userId + " data 不应为空");

        for (int i = 0; i < pnlData.size(); i++) {
            JSONObject item     = pnlData.getJSONObject(i);
            int    itemType     = item.getIntValue("type");         // 1 / 7 / 30
            BigDecimal rIncome  = item.getBigDecimal("profitAndLoss");
            BigDecimal rRatio   = item.getBigDecimal("profitAndLossRatio");

            String tag = "用户：" + userId + " type=" + itemType;

            BigDecimal[] localPnl  = MultiCurrencySwapIncomeCalculate.getCyclicalPnL(userId, itemType, currentEnv);
            BigDecimal localIncome = localPnl[0];
            BigDecimal localRatio  = localPnl[1];

            Assert.assertTrue(
                    rIncome.subtract(localIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " 全币种盈亏不一致，测试：" + rIncome + "，预期：" + localIncome);
            Assert.assertTrue(
                    rRatio.subtract(localRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                    tag + " 全币种盈亏率不一致，测试：" + rRatio + "，预期：" + localRatio);
        }
    }
}
