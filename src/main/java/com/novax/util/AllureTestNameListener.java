package com.novax.util;

import io.qameta.allure.Allure;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;

public class AllureTestNameListener implements IHookable {

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        try {
            Object[] parameters = testResult.getParameters();
            if (parameters != null && parameters.length >= 3) {
                String caseNum = String.valueOf(parameters[0]);
                String caseName = String.valueOf(parameters[1]);
                String caseDescription = String.valueOf(parameters[2]);
                String dynamicName = String.format("%s-%s-%s", caseName, caseNum, caseDescription);
                Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid ->
                    Allure.getLifecycle().updateTestCase(uuid, r -> r.setName(dynamicName))
                );
            }
        } catch (Exception ignored) {
        }
        callBack.runTestMethod(testResult);
    }
}
