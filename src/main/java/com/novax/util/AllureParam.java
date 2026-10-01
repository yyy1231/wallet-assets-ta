package com.novax.util;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;

public class AllureParam {
    public static <T> T set(String name, T value) {
        if (name == null) return value;
        try {
            Allure.getLifecycle().updateTestCase(result -> {
                result.getParameters().removeIf(p -> name.equals(p.getName()));
                Parameter param = new Parameter();
                param.setName(name);
                param.setValue(value == null ? "" : String.valueOf(value));
                result.getParameters().add(param);
            });
        } catch (Exception ignored) {
        }
        return value;
    }
}
