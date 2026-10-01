package com.novax.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class urlProperty {
    private static final Properties properties = new Properties();

    static {
        try (InputStream resourceAsStream = PropertyUtil.class.getResourceAsStream("/url.properties")) {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getWalletotalTotal() {
        return properties.getProperty("walletotal.total");
    }

    public static String getWalletotalCurrency() {
        return properties.getProperty("walletotal.currency");
    }

    public static String getWalletotalTrend() {
        return properties.getProperty("walletotal.trend");
    }

    public static String getWalletotalProfit() {
        return properties.getProperty("walletotal.profit");
    }

    public static String getSpotAssets() {
        return properties.getProperty("spot.assets");
    }
    public static String getCurrencyPrifit() {
        return properties.getProperty("currency.profit");
    }

    public static String getSpotTrend() {
        return properties.getProperty("spot.trend");
    }
    public static String getContractTrend() {
        return properties.getProperty("contract.trend");
    }

    public static String getContractAssets() {
        return properties.getProperty("contract.assets");
    }

    public static String getMultiTrend() {
        return properties.getProperty("multi.trend");
    }

    public static String getMultiAssets() {
        return properties.getProperty("multi.assets");
    }

    public static String getMultiPositions() {
        return properties.getProperty("multi.positions");
    }

    public static String getCopyProfits() {
        return properties.getProperty("copy.profits");
    }

    public static String getCopyPositions() {
        return properties.getProperty("copy.positions");
    }

    public static String getCopyFollows() {
        return properties.getProperty("copy.follows");
    }

    public static String getP2pAssets() {
        return properties.getProperty("p2p.assets");
    }

    public static String getBotTotal() {
        return properties.getProperty("bot.total");
    }

    public static String getBotOrders() {
        return properties.getProperty("bot.orders");
    }

    public static String getBotAssets() {
        return properties.getProperty("bot.assets");
    }

    public static String getFinanceOverview() {
        return properties.getProperty("finance.overview");
    }

    public static String getFinanceProducts() {
        return properties.getProperty("finance.products");
    }
}
