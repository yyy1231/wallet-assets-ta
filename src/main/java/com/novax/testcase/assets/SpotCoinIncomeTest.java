package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.SpotCoinIncomeCalculate;
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

public class SpotCoinIncomeTest extends BaseTest {
    public static Logger logger = Logger.getLogger(SpotCoinIncomeTest.class);
    @DataProvider(name = "datas", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/SpotCoinIncomeTest.xls", 0), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "单币种今日盈亏/盈亏率")
    public void spotCoinIncome_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getBigdataHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 币种太多 仅断言btc
        String currency = "btc";
        // 业务断言：单币种今日盈亏/盈亏率 & 累计收益/收益率
        BigDecimal[] localPnl = SpotCoinIncomeCalculate.getCoinPnL(userId, currency, currentEnv);
        BigDecimal localTodayIncome = localPnl[0];
        BigDecimal localTodayRatio = localPnl[1];
        BigDecimal localCumIncome = localPnl[2];
        BigDecimal localCumRatio = localPnl[3];

        JSONArray coinData = resp.getJSONArray("data");
        BigDecimal rTodayIncome = BigDecimal.ZERO;
        BigDecimal rTodayRatio = BigDecimal.ZERO;
        BigDecimal rCumIncome = BigDecimal.ZERO;
        BigDecimal rCumRatio = BigDecimal.ZERO;
        if (coinData != null) {
            for (int i = 0; i < coinData.size(); i++) {
                JSONObject item = coinData.getJSONObject(i);
                if (currency.equals(item.getString("currency"))) {
                    rTodayIncome = item.getBigDecimal("profitAndLossDay");
                    rTodayRatio = item.getBigDecimal("profitAndLossRatioDay");
                    rCumIncome = item.getBigDecimal("profitAndLoss");
                    rCumRatio = item.getBigDecimal("profitAndLossRatio");
                    break;
                }
            }
        }

        Assert.assertTrue(rTodayIncome.subtract(localTodayIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                "用户：" + userId + " currency=" + currency + " 今日盈亏不一致，测试：" + rTodayIncome + "，预期：" + localTodayIncome);
        Assert.assertTrue(rTodayRatio.subtract(localTodayRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                "用户：" + userId + " currency=" + currency + " 今日盈亏率不一致，测试：" + rTodayRatio + "，预期：" + localTodayRatio);
        Assert.assertTrue(rCumIncome.subtract(localCumIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                "用户：" + userId + " currency=" + currency + " 累计收益不一致，测试：" + rCumIncome + "，预期：" + localCumIncome);
        Assert.assertTrue(rCumRatio.subtract(localCumRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                "用户：" + userId + " currency=" + currency + " 累计收益率不一致，测试：" + rCumRatio + "，预期：" + localCumRatio);
    }
}
