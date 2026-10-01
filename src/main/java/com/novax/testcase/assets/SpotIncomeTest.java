package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.BaseTest;
import com.novax.testcase.SpotCoinIncomeCalculate;
import com.novax.testcase.SpotIncomeCalculate;
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

public class SpotIncomeTest extends BaseTest {
    public static Logger logger = Logger.getLogger(SpotIncomeTest.class);

    @DataProvider(name = "total", parallel = true)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/SpotIncomeTest.xls", 0), currentEnv);
    }

    @DataProvider(name = "profit", parallel = true)
    public Object[][] getProvider2() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/SpotIncomeTest.xls", 1), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "总资产估值&单币维度资产统计")
    public void spot_Total_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getSpotHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendGet(url, null, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        JSONObject data = resp.getJSONObject("data");
        BigDecimal totalU = data.getBigDecimal("valuationCurrency");
        BigDecimal totalBtc = data.getBigDecimal("valuationCurrencyBtc");
        JSONArray assetList = data.getJSONArray("assetList");
        //  过滤掉balance=0的数据
        assetList.removeIf(obj -> {
            JSONObject json = (JSONObject) obj;
            BigDecimal balance = json.getBigDecimal("balance");
            return balance == null || balance.compareTo(BigDecimal.ZERO) == 0;
        });

        // 预期
        BigDecimal expectTotalU = SpotIncomeCalculate.getCurrentSpotAccount(userId, currentEnv);
        BigDecimal expectTotalBtc = NewestPriceUtil.toBtcAmount(totalU, currentEnv);
        Map<String, SpotCoinIncomeCalculate.SpotCoinDetail> assertsDetail =
                SpotCoinIncomeCalculate.getSpotCoinDetailMap(userId, currentEnv);

        for (int i = 0; i < assetList.size(); i++) {
            JSONObject item = assetList.getJSONObject(i);
            String currency = item.getString("currency");
            BigDecimal balance = item.getBigDecimal("balance");
            BigDecimal available = item.getBigDecimal("available");
            BigDecimal frozen = item.getBigDecimal("frozen");
            BigDecimal valuationCurrencyBtc = item.getBigDecimal("valuationCurrencyBtc");
            BigDecimal pnlDay = item.getBigDecimal("profitAndLossDay");
            BigDecimal pnlRatioDay = item.getBigDecimal("profitAndLossRatioDay");
            BigDecimal pnl = item.getBigDecimal("profitAndLoss");
            BigDecimal pnlRatio = item.getBigDecimal("profitAndLossRatio");
            BigDecimal avg = item.getBigDecimal("avg");
            int inCalculation = item.getIntValue("inCalculation");

            String tag = "用户：" + userId + " 币种[" + currency + "]";

            // 1. currency 非空
            Assert.assertNotNull(currency, tag + " currency 不应为 null");
            Assert.assertFalse(currency.trim().isEmpty(), tag + " currency 不应为空字符串");

            String key = currency.toLowerCase();
            SpotCoinIncomeCalculate.SpotCoinDetail currencyDetail =
                    assertsDetail.getOrDefault(key, new SpotCoinIncomeCalculate.SpotCoinDetail());

            // 2. balance ≈ 本地总持仓量
            Assert.assertTrue(
                    balance.subtract(currencyDetail.balance).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " balance 偏差超限，测试：" + balance + "，预期：" + currencyDetail.balance);

            // 3. available ≈ 本地可用量（含 ET/SCS 净额）
            Assert.assertTrue(
                    available.subtract(currencyDetail.available).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " available 偏差超限，测试：" + available + "，预期：" + currencyDetail.available);

            // 4. frozen ≈ 本地冻结量
            Assert.assertTrue(
                    frozen.subtract(currencyDetail.frozen).abs().compareTo(COIN_TOLERANT) <= 0,
                    tag + " frozen 偏差超限，测试：" + frozen + "，预期：" + currencyDetail.frozen);

            // 5. valuationCurrencyBtc ≈ toBtcAmount(balance, currency, env)
            BigDecimal expectedBtc = NewestPriceUtil.toBtcAmount(balance, currency.toLowerCase(), currentEnv);
            Assert.assertTrue(valuationCurrencyBtc.subtract(expectedBtc).abs()
                            .compareTo(BTC_TOLERANT) <= 0,
                    tag + " valuationCurrencyBtc 偏差超限，测试：" + valuationCurrencyBtc + "，预期：" + expectedBtc);

            // 币种usdt，不需要计算PnL / avg
            if (currency.equals("usdt")) {
                logger.warn(tag + "currency=usdt，跳过 PnL / avg 断言");
                continue;
            }

            // 6~10. PnL 及均价：inCalculation=0 正常；1 计算中，跳过断言
            if (inCalculation == 1) {
                logger.warn(tag + "inCalculation=1计算中，跳过 PnL / avg 断言");
                continue;
            }

            // 6. avg 成本价
            if (currencyDetail.avg.compareTo(BigDecimal.ZERO) > 0) {
                Assert.assertTrue(
                        avg.subtract(currencyDetail.avg).abs().compareTo(COIN_TOLERANT) <= 0,
                        tag + " avg 偏差超限，测试：" + avg + "，预期：" + currencyDetail.avg);}

            // 7. profitAndLoss 持有收益
            Assert.assertTrue(
                    pnl.subtract(currencyDetail.pnlCumulative).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " profitAndLoss持有收益 偏差超限，测试：" + pnl + "，预期：" + currencyDetail.pnlCumulative);

            // 8. profitAndLossRatio 持有收益率
            Assert.assertTrue(
                    pnlRatio.subtract(currencyDetail.pnlCumulativeRatio).abs().compareTo(RATIO_TOLERANT) <= 0,
                    tag + " profitAndLossRatio持有收益率 偏差超限，测试：" + pnlRatio + "，预期：" + currencyDetail.pnlCumulativeRatio);

            // 9. profitAndLossDay 今日盈亏
            Assert.assertTrue(
                    pnlDay.subtract(currencyDetail.pnlDay).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " profitAndLossDay 偏差超限，测试：" + pnlDay + "，预期：" + currencyDetail.pnlDay);

            // 10. profitAndLossRatioDay 今日盈亏率
            Assert.assertTrue(
                    pnlRatioDay.subtract(currencyDetail.pnlRatioDay).abs().compareTo(RATIO_TOLERANT) <= 0,
                    tag + " profitAndLossRatioDay 偏差超限，测试：" + pnlRatioDay + "，预期：" + currencyDetail.pnlRatioDay);

             }

        Assert.assertTrue(totalU.subtract(expectTotalU).abs().compareTo(USDT_TOLERANT) <= 0,
                "用户：" + userId + " 现货账户资产汇总(折U)计算有误，测试：" + totalU + "，预期：" + expectTotalU);
        Assert.assertTrue(totalBtc.subtract(expectTotalBtc).abs().compareTo(BTC_TOLERANT) <= 0,
                "用户：" + userId + " 现货账户资产汇总(折Btc)计算有误，测试：" + totalBtc + "，预期：" + expectTotalBtc);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "profit", description = "现货盈亏/盈亏率")
    public void spotIncome_Normal_Test(String tcNum, String tcName, String tcDescp, String reqAddress, String headers, String params, String expect) throws Exception {
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

            if (inCalculation == 1) {
                logger.warn(tag + " inCalculation=1，跳过盈亏断言");
                continue;
            }

            // 预期
            BigDecimal[] localPnl = SpotIncomeCalculate.getCyclicalPnL(userId, itemType, currentEnv);
            BigDecimal localIncome = localPnl[0];
            BigDecimal localRatio = localPnl[1];

            Assert.assertTrue(
                    rIncome.subtract(localIncome).abs().compareTo(PNL_TOLERANT) <= 0,
                    tag + " 现货盈亏不一致，测试：" + rIncome + "，预期：" + localIncome);
            Assert.assertTrue(
                    rRatio.subtract(localRatio).abs().compareTo(RATIO_TOLERANT.multiply(BigDecimal.TEN)) <= 0,
                    tag + " 现货盈亏率不一致，测试：" + rRatio + "，预期：" + localRatio);
        }
    }
}
