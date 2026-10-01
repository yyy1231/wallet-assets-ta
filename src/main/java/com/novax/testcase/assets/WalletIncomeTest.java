package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.TestUserConfig;
import com.novax.testcase.WalletIncomeCalculate;
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
import java.util.Map;

public class WalletIncomeTest extends BaseTest {
    public static Logger logger = Logger.getLogger(WalletIncomeTest.class);

    @DataProvider(name = "total", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/WalletIncomeTest.xls", 0), currentEnv);
    }

    @DataProvider(name = "currency", parallel = true)
    public Object[][] getProvider2() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/WalletIncomeTest.xls", 1), currentEnv);
    }

    @DataProvider(name = "trend", parallel = true)
    public Object[][] getProvider3() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/WalletIncomeTest.xls", 2), currentEnv);
    }

    @DataProvider(name = "profit", parallel = true)
    public Object[][] getProvider4() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/WalletIncomeTest.xls", 3), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "总资产估值&各账户资产汇总")
    public void wallet_Total_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));
        String url = PropertyUtil.getWalletHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        JSONObject data = resp.getJSONObject("data");
        BigDecimal totalUsd = data.getBigDecimal("totalUsd");
        BigDecimal totalBtc = data.getBigDecimal("totalBtc");
        // 子账户资产列表
        // 账户类型：1=现货，2=c2c，3=U本位合约，4=币本位账户，5=超级合约，6=理财账户，7=自由区账户，8=现货网格账户，9=AMM账户，10=合约跟单账户
        JSONArray assetList = data.getJSONArray("assetList");

        // 业务断言 资产汇总
        BigDecimal currentWalletTotalU = WalletIncomeCalculate.getCurrentWalletTotal(userId, currentEnv);
        BigDecimal expectedBtc = WalletIncomeCalculate.getCurrentWalletTotalBtc(currentWalletTotalU, currentEnv);
        Assert.assertTrue(totalBtc.subtract(expectedBtc).abs().compareTo(BTC_TOLERANT) <= 0,
                "用户：" + userId + " totalBtc偏差超限，测试：" + totalBtc + "，预期：" + expectedBtc);
        Assert.assertTrue(totalUsd.subtract(currentWalletTotalU).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " 资产总览计算有误，测试：" + totalUsd + "，预期：" + currentWalletTotalU);
        // 子账户资产断言
        Map<Integer, BigDecimal> expectedMap = WalletIncomeCalculate.getSubAccountExpectedMap(userId, currentEnv);

        for (int i = 0; i < assetList.size(); i++) {
            JSONObject item = assetList.getJSONObject(i);
            int type = item.getIntValue("type");
            BigDecimal actualUsd = item.getBigDecimal("totalUsd");

            if (!expectedMap.containsKey(type)) {
                logger.warn("用户：" + userId + " 子账户 type=" + type + " 暂无本地计算来源，跳过断言");
                continue;
            }
            BigDecimal expectedUsd = expectedMap.get(type);
            Assert.assertTrue(
                    actualUsd.subtract(expectedUsd).abs().compareTo(USDT_TOLERANT) <= 0,
                    "用户：" + userId + " 子账户 type=" + type
                            + " 资产偏差超限，测试：" + actualUsd + "，预期：" + expectedUsd);
        }
    }

    @Test(groups = {"smokeTest"}, dataProvider = "profit", description = "周期盈亏/盈亏率")
    public void wallet_Profit_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        long userId = resolveUserId(reqHeaders.get("userId"));
        int type = Integer.parseInt(reqParams.get("type"));

        String url = PropertyUtil.getBigdataHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 业务断言：周期盈亏 & 盈亏率
        // 已知：本地 thirtyUtc 取31天前UTC零点，后端可能取整30天或自然月；type=30偶发偏差先查起点对齐
        BigDecimal[] localPnl = WalletIncomeCalculate.getCyclicalPnL(userId, type, currentEnv);
        BigDecimal localIncome = localPnl[0];
        BigDecimal localRatio = localPnl[1];
        logger.info("用户：" + userId + " type=" + type + " 周期盈亏：" + localIncome);
        logger.info("用户：" + userId + " type=" + type + " 周期盈亏率：" + localRatio);

        JSONArray pnlData = resp.getJSONArray("data");
        JSONObject item = pnlData.getJSONObject(0);
        BigDecimal rIncome = item.getBigDecimal("profitAndLoss");
        BigDecimal rRatio = item.getBigDecimal("profitAndLossRatio");

        Assert.assertTrue(localIncome.subtract(rIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                "用户：" + userId + " type=" + type + " 周期盈亏不一致，测试：" + rIncome + "，预期：" + localIncome);
        Assert.assertTrue(
                localRatio.subtract(rRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                "用户：" + userId + " type=" + type + " 盈亏率不一致，测试：" + rRatio + "，预期：" + localRatio + " 盈亏率出现0/100，通常是因为大数据期初资产缺失，脚本同后端处理逻辑有差异，导致断言不过，问题不大可忽略");
    }

    @Test(groups = {"smokeTest"}, dataProvider = "trend", description = "总资产趋势图")
    public void wallet_Trend_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
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
        String period = reqParams.getOrDefault("type", "1");

        // 业务断言
        // 历史快照无法本地复现，改为结构性断言：数据点数 + 非负值 新用户或没有7/30天数据 不断言数据点数了
        // 截至今日零点 user_assets_statistics_wallet_day:total_usdt+position_usdt
        if (period.equals("7") || period.equals("30")) {
            int size = trendData.size();
            int points = "7".equals(period) ? 7 : 30;
            int minPoints = Math.min(size, points);
            for (int i = 0; i < minPoints; i++) {
                BigDecimal p = trendData.getJSONObject(i).getBigDecimal("usdtPrice");
                Assert.assertTrue(p != null && p.compareTo(BigDecimal.ZERO) >= 0,
                        "用户：" + userId + " 趋势图[" + i + "] usdtPrice 异常：" + p);
            }
        }
        // 趋势图最新快照 ≈ 当前总资产
        if (period.equals("1")) {
            BigDecimal expectedTotal = WalletIncomeCalculate.getCurrentWalletTotal(userId, currentEnv);
            if (trendData.size() == 0) {
                logger.info("用户：" + userId + "10min数据空，接口异常或新建用户暂无10min数据");
            } else {
                JSONObject latestPoint = trendData.getJSONObject(0);
                BigDecimal usdtAmount = latestPoint.getBigDecimal("usdtPrice");
                Assert.assertTrue(usdtAmount.subtract(expectedTotal).abs().compareTo(USDT_TOLERANT) <= 0,
                        "用户：" + userId + " 趋势图最新快照与当前总资产偏差超限，测试：" + usdtAmount + "，预期：" + expectedTotal);
            }
        }
    }

    @Test(groups = {"smokeTest"}, dataProvider = "currency", description = "按币种维度聚合各账户资产")
    public void wallet_Currency_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getWalletHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        JSONObject data = resp.getJSONObject("data");
        BigDecimal totalUsd = data.getBigDecimal("totalUsd");
        BigDecimal totalBtc = data.getBigDecimal("totalBtc");
        JSONArray currencyList = data.getJSONArray("assetList");

        // 业务断言：各币种 USDT 汇总 ≈ 当前总资产
        BigDecimal expectUsd = WalletIncomeCalculate.getCurrentWalletTotal(userId, currentEnv);
        BigDecimal expectedBtc = WalletIncomeCalculate.getCurrentWalletTotalBtc(expectUsd, currentEnv);
        // Assert.assertTrue(totalBtc.subtract(expectedBtc).abs().compareTo(BTC_TOLERANT) <= 0,
        //         "用户：" + userId + " totalBtc偏差超限，测试：" + totalBtc + "，预期：" + expectedBtc);
        // Assert.assertTrue(totalUsd.subtract(expectUsd).abs().compareTo(USDT_TOLERANT) <= 0,
        //         "用户：" + userId + " 币种汇总与总资产偏差超限，测试：" + totalUsd + "，预期：" + expectUsd);

        // assetList 按币种维度聚合各账户资产 按币种断言：currency、total、totalUsd、totalBtc、assetRatio
        Map<String, WalletIncomeCalculate.WalletCurrencyDetail> localCurrencyMap =
                WalletIncomeCalculate.getWalletCurrencyMap(userId, currentEnv);
        for (int i = 0; i < currencyList.size(); i++) {
            JSONObject item = currencyList.getJSONObject(i);
            String currency = item.getString("currency");
            BigDecimal itemUsd = item.getBigDecimal("totalUsd");

            // 1. currency 非空
            Assert.assertNotNull(currency,
                    "用户：" + userId + " assetList[" + i + "] currency 不应为 null");
            Assert.assertFalse(currency.trim().isEmpty(),
                    "用户：" + userId + " assetList[" + i + "] currency 不应为空字符串");

            WalletIncomeCalculate.WalletCurrencyDetail localDetail =
                    localCurrencyMap.getOrDefault(currency.toLowerCase(), new WalletIncomeCalculate.WalletCurrencyDetail());

            // 2. total ≈ 本地原始数量
            BigDecimal itemTotal = item.getBigDecimal("total");
            Assert.assertTrue(
                    itemTotal.subtract(localDetail.total).abs().compareTo(COIN_TOLERANT) <= 0,
                    "用户：" + userId + " 币种[" + currency + "] total 偏差超限，测试：" + itemTotal + "，预期：" + localDetail.total);

            // 3. totalUsd ≈ 本地按币种计算值
            Assert.assertTrue(
                    itemUsd.subtract(localDetail.totalUsd).abs().compareTo(USDT_TOLERANT) <= 0,
                    "用户：" + userId + " 币种[" + currency + "] totalUsd 偏差超限，测试：" + itemUsd + "，预期：" + localDetail.totalUsd);

            // 4. totalBtc ≈ 本地 toBtcAmount 折算值
            BigDecimal itemBtc = item.getBigDecimal("totalBtc");
            Assert.assertTrue(
                    itemBtc.subtract(localDetail.totalBtc).abs().compareTo(BTC_TOLERANT) <= 0,
                    "用户：" + userId + " 币种[" + currency + "] totalBtc 偏差超限，测试：" + itemBtc + "，预期：" + localDetail.totalBtc);

            // 5. assetRatio ≈ 本地计算值
            BigDecimal assetRatio = item.getBigDecimal("assetRatio");
            Assert.assertTrue(
                    assetRatio.subtract(localDetail.assetRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                    "用户：" + userId + " 币种[" + currency + "] assetRatio 偏差超限，测试：" + assetRatio + "，预期：" + localDetail.assetRatio);
        }
    }
}
