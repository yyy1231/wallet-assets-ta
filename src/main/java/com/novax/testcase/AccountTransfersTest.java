package com.novax.testcase;

import com.alibaba.fastjson.JSONObject;
import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.PropertyUtil;
import com.novax.util.StringToMap;
import org.apache.log4j.MDC;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;

public class AccountTransfersTest extends BaseTest {
    @DataProvider(name = "total", parallel = false)
    public Object[][] getProvider() {
        return TestUserConfig.expand(ExcelUtil.readObjDatas("/testcase/AccountTransfersTest.xls", 0), currentEnv);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "total", description = "账户资金划转")
    public void account_Transfer_Test(String tcNum, String tcName, String tcDescp,
                                      String reqAddress, String headers, String params, String expect) throws Exception {
        MDC.put("caseID", "thread" + Thread.currentThread().getId() + "-" + tcNum + "-" + tcDescp);

        // current、target 1=币币账户；2=法币账户；3=永续合约币本位；4=永续合约U本位；5=全币种合约; 6=理财; 10:企业账户
        JSONObject reqParams = JSONObject.parseObject(params);
        HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
        long userId = resolveUserId(reqHeaders.get("userId"));

        String url = PropertyUtil.getWalletHost(currentEnv) + reqAddress;
        JSONObject resp = HttpClientUtil.sendPostByJson(url, reqParams, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), "用户" + userId + resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
    }

}
