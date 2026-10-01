package com.novax.util;

import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class AllureParamRenamer implements ITestListener {

    private static final String[] RENAME_MAP =
            {"tcNum", "tcName", "tcDescp", "reqAddress", "headers", "params", "expect"};

    @Override
    public void onTestStart(ITestResult result) {
        try {
            Allure.getLifecycle().updateTestCase(testResult -> {
                if (testResult.getParameters() == null) return;
                for (int i = 0; i < RENAME_MAP.length; i++) {
                    final String argName = "arg" + i;
                    final String newName = RENAME_MAP[i];
                    testResult.getParameters().stream()
                            .filter(p -> argName.equals(p.getName()))
                            .findFirst()
                            .ifPresent(p -> p.setName(newName));
                }
            });
        } catch (Exception ignored) {
        }
    }
}
