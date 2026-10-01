package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.SwapIncomeCalculate;
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
import java.util.Map;

public class SwapIncomeTest extends BaseTest {
    public static Logger logger = Logger.getLogger(SwapIncomeTest.class);
    @DataProvider(name = "total", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/SwapIncomeTest.xls", 0), currentEnv);
    }

    @DataProvider(name = "profit", parallel = true)
    public Object[][] getProvider2() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/SwapIncomeTest.xls", 1), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "合约总资产估值&资产列表")
    public void swap_Total_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getSwapHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        JSONObject data = resp.getJSONObject("data");
        BigDecimal totalUsdt = data.getBigDecimal("valuationCurrency");
        // TODO 后端逻辑 不包含et资产 ∑swap_front_user_assets.available
        BigDecimal totalWalletBalance = data.getBigDecimal("walletBalance");
        BigDecimal totalPnl = data.getBigDecimal("profitLoss");
        JSONArray assetList = data.getJSONArray("assetList");
        //  过滤掉walletBalance=0的数据
        assetList.removeIf(obj -> {
            JSONObject json = (JSONObject) obj;
            BigDecimal walletBalance = json.getBigDecimal("walletBalance");
            return walletBalance == null || walletBalance.compareTo(BigDecimal.ZERO) == 0;
        });

        // 业务断言：合约全部资产（账户余额 + 未实现盈亏）≈ 当前实时计算值
        BigDecimal expectTotalU = SwapIncomeCalculate.getCurrentSwapTotal(userId, currentEnv);
        Assert.assertTrue(
                totalUsdt.subtract(expectTotalU).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " 合约总资产计算有误，测试：" + totalUsdt + "，预期：" + expectTotalU);

        Map<String, SwapIncomeCalculate.SwapSymbolDetail> detailMap =
                SwapIncomeCalculate.getSwapSymbolDetailMap(userId, currentEnv);

        // totalWalletBalance ≈ 非ET账户的 walletBalance 之和
        BigDecimal expectTotalWb = detailMap.values().stream()
                .filter(d -> !"et".equals(d.currency))
                .map(d -> d.walletBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assert.assertTrue(
                totalWalletBalance.subtract(expectTotalWb).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " totalWalletBalance 偏差超限，测试：" + totalWalletBalance + "，预期：" + expectTotalWb);

        // totalPnl ≈ 非ET账户的 profitLoss 之和
        BigDecimal expectTotalPnl = detailMap.values().stream()
                .filter(d -> !"et".equals(d.currency))
                .map(d -> d.profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assert.assertTrue(
                totalPnl.subtract(expectTotalPnl).abs().compareTo(PNL_TOLERANT) <= 0,
                "用户：" + userId + " totalPnl 偏差超限，测试：" + totalPnl + "，预期：" + expectTotalPnl);

        for (int i = 0; i < assetList.size(); i++) {
            JSONObject item = assetList.getJSONObject(i);
            String symbol = item.getString("symbol");
            BigDecimal available = item.getBigDecimal("available");
            BigDecimal profitLoss = item.getBigDecimal("profitLoss");
            BigDecimal marginBalance = item.getBigDecimal("marginBalance");
            BigDecimal walletBalance = item.getBigDecimal("walletBalance");
            String currency = item.getString("currency");

            String tag = "用户：" + userId + " 合约[" + symbol + "]";

            // 1. symbol 非空
            Assert.assertNotNull(symbol, tag + " symbol 不应为 null");
            Assert.assertFalse(symbol.trim().isEmpty(), tag + " symbol 不应为空字符串");

            SwapIncomeCalculate.SwapSymbolDetail local =
                    detailMap.getOrDefault(symbol, new SwapIncomeCalculate.SwapSymbolDetail());

            // 2. available ≈ 本地可划转余额
            Assert.assertTrue(
                    available.subtract(local.available).abs().compareTo(USDT_TOLERANT) <= 0,
                    tag + " available 偏差超限，测试：" + available + "，预期：" + local.available);

            // 3. profitLoss ≈ 本地按 symbol 持仓未实现盈亏
            Assert.assertTrue(
                    profitLoss.subtract(local.profitLoss).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " profitLoss 偏差超限，测试：" + profitLoss + "，预期：" + local.profitLoss);

            // 4. marginBalance ≈ 本地 (available+frozen) + profitLoss（容差 1 USDT）
            Assert.assertTrue(
                    marginBalance.subtract(local.marginBalance).abs().compareTo(USDT_TOLERANT) <= 0,
                    tag + " marginBalance 偏差超限，测试：" + marginBalance + "，预期：" + local.marginBalance);

            // 5. walletBalance ≈ 本地 available+frozen（容差 1e-6）
            Assert.assertTrue(
                    walletBalance.subtract(local.walletBalance).abs().compareTo(USDT_TOLERANT) <= 0,
                    tag + " walletBalance 偏差超限，测试：" + walletBalance + "，预期：" + local.walletBalance);

            // 6. currency = 本地保证金币种（type=3 均为 usdt，type=2 为 et）
            if (currency != null) {
                Assert.assertEquals(currency.toLowerCase(), local.currency,
                        tag + " currency 与本地不符");
            }
        }
    }

    @Test(groups = {"smokeTest"}, dataProvider = "profit", description = "合约盈亏/盈亏率")
    public void swapIncome_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
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

        // 业务断言：遍历三个周期（type=1/7/30），逐一断言
        JSONArray pnlData = resp.getJSONArray("data");
        Assert.assertNotNull(pnlData, "用户：" + userId + " data 不应为 null");
        Assert.assertFalse(pnlData.isEmpty(), "用户：" + userId + " data 不应为空");

        for (int i = 0; i < pnlData.size(); i++) {
            JSONObject item = pnlData.getJSONObject(i);
            int itemType = item.getIntValue("type");          // 1 / 7 / 30
            int inCalculation = item.getIntValue("inCalculation");
            BigDecimal rIncome = item.getBigDecimal("profitAndLoss");
            BigDecimal rRatio = item.getBigDecimal("profitAndLossRatio");

            String tag = "用户：" + userId + " type=" + itemType;

            // TODO 计算中场景 跳过or断言不过
            if (inCalculation == 1) {
                logger.error(tag + " inCalculation=1计算中，跳过盈亏断言");
                continue;
            }

            BigDecimal[] localPnl = SwapIncomeCalculate.getCyclicalPnL(userId, itemType, currentEnv);
            BigDecimal localIncome = localPnl[0];
            BigDecimal localRatio = localPnl[1];

            Assert.assertTrue(
                    rIncome.subtract(localIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " 合约盈亏不一致，测试：" + rIncome + "，预期：" + localIncome);
            Assert.assertTrue(
                    rRatio.subtract(localRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                    tag + " 合约盈亏率不一致，测试：" + rRatio + "，预期：" + localRatio);
        }
    }
}
