package com.novax.testcase;

import com.novax.entity.mysql.OtcUserAssetsEntity;
import com.novax.util.NewestPriceUtil;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OTC（P2P）账户资产分析：按币种汇总持仓快照，折算 USDT 估值，供测试类断言。
 */
public class OtcAssetsCalculate extends BaseTest {

    /**
     * 查询 OTC 账户 USDT 总额（所有币种 usdtValue 之和）。
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return OTC 账户 USDT 总额
     */
    public static BigDecimal getCurrentOtcAccount(long userId, String env) {
        BigDecimal total = getOtcCoinDetailMap(userId, env).values().stream()
                .map(d -> d.usdtValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.warn("OTC 账户 USDT 总额：" + total);
        return total;
    }

    /**
     * 按币种构建 OTC 账户本地资产快照 map，与 /v3/assets/all assetList 可断言字段对应。
     * key = 币种小写，value = {@link OtcCoinDetail}
     *
     * @param userId 用户ID
     * @param env    目标环境（test/dev）
     * @return 币种小写 → OtcCoinDetail
     */
    public static Map<String, OtcCoinDetail> getOtcCoinDetailMap(long userId, String env) {
        Map<String, OtcCoinDetail> map = new LinkedHashMap<>();
        List<OtcUserAssetsEntity> assets = UserAssetsManage.queryOtcAssets(userId);
        for (OtcUserAssetsEntity a : assets) {
            String key = a.getCurrency().toLowerCase();
            OtcCoinDetail d = map.computeIfAbsent(key, k -> new OtcCoinDetail());

            BigDecimal available = a.getAvailable() == null ? BigDecimal.ZERO : a.getAvailable();
            BigDecimal assure = a.getCashDeposit() == null ? BigDecimal.ZERO : a.getCashDeposit();
            BigDecimal frozen = a.getFrozen() == null ? BigDecimal.ZERO : a.getFrozen();

            d.available = d.available.add(available);
            d.assure = d.assure.add(assure);
            d.frozen = d.frozen.add(frozen);
            d.totalFrozen = d.frozen.add(d.assure);
            d.totalAmount = d.available.add(d.assure).add(d.frozen);
            d.usdtValue = d.totalAmount.multiply(
                    NewestPriceUtil.newPriceOfSymbol(a.getCurrency() + "_usdt", env));
        }
        logger.info("OtcCoinDetailMap size=" + map.size());
        return map;
    }

    /**
     * OTC 币种本地资产快照，用于与 /v3/assets/all assetList 逐字段断言对比。
     */
    public static class OtcCoinDetail {
        public BigDecimal available = BigDecimal.ZERO; // DB available
        public BigDecimal assure = BigDecimal.ZERO; // DB assure（OTC 保证金）
        public BigDecimal frozen = BigDecimal.ZERO; // DB frozen
        public BigDecimal totalFrozen = BigDecimal.ZERO; // DB frozen
        public BigDecimal totalAmount = BigDecimal.ZERO; // available + assure + frozen
        public BigDecimal usdtValue = BigDecimal.ZERO; // totalAmount × Redis 价
    }
}
