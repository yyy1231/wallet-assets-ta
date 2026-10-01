package com.novax.testcase;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.mysql.SwapUserAssetsEntity;
import com.novax.util.MongoUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 通用工具方法集合：数值舍入截取、MongoDB 类型转换、时间转换、查询封装，
 * 以及合约/全币种合约盈亏计算与周期盈亏断言。
 */
public class UtilityTools {
    private static final Logger logger = Logger.getLogger(UtilityTools.class);

    /**
     * 四舍五入到指定小数位。
     *
     * @param number 原始值，为 null 或 ZERO 时直接返回 ZERO
     * @param scale  保留小数位数
     * @return 四舍五入后的 BigDecimal
     */
    public static BigDecimal round(BigDecimal number, int scale) {
        if (number == null || number.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return number.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 截取小数，不四舍五入。
     *
     * @param number 原始值，为 null 或 ZERO 时直接返回 ZERO
     * @param scale  保留小数位数
     * @return 截取后的 BigDecimal（向 ZERO 方向舍弃多余小数）
     */
    public static BigDecimal truncate(BigDecimal number, int scale) {
        if (number == null || number.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return number.setScale(scale, RoundingMode.DOWN);
    }

    /**
     * 将任意 MongoDB 字段值转为 BigDecimal。
     * 支持 String、BSON Decimal128、Double、Long、Integer。
     *
     * @param val MongoDB 文档字段原始值，为 null 时返回 ZERO
     * @return 对应的 BigDecimal 值
     */
    public static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Decimal128) return ((Decimal128) val).bigDecimalValue();
        if (val instanceof Double) return BigDecimal.valueOf((Double) val);
        if (val instanceof Long) return BigDecimal.valueOf((Long) val);
        if (val instanceof Integer) return BigDecimal.valueOf((Integer) val);
        return new BigDecimal(val.toString());
    }

    /**
     * 将 LocalDateTime 作为 UTC 时间转换为 java.util.Date。
     * MongoDB 时间戳视为 UTC（无时区信息）。
     *
     * @param ldt 被视为 UTC 时区的本地时间
     * @return 对应的 UTC java.util.Date
     */
    public static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    /**
     * 带可选排序的 MongoDB 查询。
     *
     * @param collection 目标 MongoDB 集合
     * @param filter     查询条件
     * @param sort       排序文档（如 {@code new Document("create_time", -1)}），为 null 时不排序
     * @return 匹配的文档列表
     */
    public static List<Document> queryMongo(MongoCollection<Document> collection,
                                            BasicDBObject filter,
                                            Document sort) {
        List<Document> docs = new ArrayList<>();
        if (sort != null) {
            collection.find(filter).sort(sort).into(docs);
        } else {
            collection.find(filter).into(docs);
        }
        return docs;
    }

    /**
     * 计算合约单条持仓的未实现盈亏（含 contractSize）。
     * direction=1 多仓: (newPrice - costOpen) / costOpen × volume × contractSize
     * direction=2 空仓: (costOpen - newPrice) / costOpen × volume × contractSize
     *
     * @param direction    仓位方向：1=多仓，2=空仓
     * @param newPrice     当前最新价
     * @param costOpen     开仓均价
     * @param volume       持仓量（手数）
     * @param contractSize 合约面值（每手对应的标的数量）
     * @return 单条持仓未实现盈亏（USDT）
     * @throws IllegalStateException direction 不为 1 或 2 时抛出
     */
    public static BigDecimal calcSwapPnl(int direction, BigDecimal newPrice, BigDecimal costOpen,
                                         BigDecimal volume, BigDecimal contractSize) {
        if (direction == 1) {
            return newPrice.subtract(costOpen)
                    .divide(costOpen, 20, RoundingMode.HALF_UP)
                    .multiply(volume)
                    .multiply(contractSize);
        } else if (direction == 2) {
            return costOpen.subtract(newPrice)
                    .divide(costOpen, 20, RoundingMode.HALF_UP)
                    .multiply(volume)
                    .multiply(contractSize);
        }
        throw new IllegalStateException("方向错误: direction=" + direction);
    }

    /**
     * 计算全币种合约单条持仓的未实现盈亏（不含 contractSize）。
     * direction=1 多仓: (newPrice - costOpen) / costOpen × volume
     * direction=2 空仓: (costOpen - newPrice) / costOpen × volume
     *
     * @param direction 仓位方向：1=多仓，2=空仓
     * @param newPrice  当前最新价
     * @param costOpen  开仓均价
     * @param volume    持仓量
     * @return 单条持仓未实现盈亏（以计价货币为单位，需再乘以对 USDT 汇率）
     * @throws IllegalStateException direction 不为 1 或 2 时抛出
     */
    public static BigDecimal calcMultiSwapPnl(int direction, BigDecimal newPrice, BigDecimal costOpen,
                                              BigDecimal volume) {
        if (direction == 1) {
            return newPrice.subtract(costOpen)
                    .divide(costOpen, 20, RoundingMode.HALF_UP)
                    .multiply(volume);
        } else if (direction == 2) {
            return costOpen.subtract(newPrice)
                    .divide(costOpen, 20, RoundingMode.HALF_UP)
                    .multiply(volume);
        }
        throw new IllegalStateException("方向错误: direction=" + direction);
    }

    /**
     * 查询 MongoDB 周期盈亏集合，汇总盈亏与盈亏率，并断言数据非空。
     *
     * @param userId            用户ID
     * @param fromUtc           起始时间（UTC），查询 create_time ≥ fromUtc 的记录
     * @param collection        MongoDB collection 名称（如 user_assets_statistics_wallet_day_profit_and_loss）
     * @param includeBeginFloat 除数是否包含期初浮动 position_usdt（现货传 false，其余传 true）
     * @param type              周期天数，仅用于日志/断言错误提示（如 7、30）
     * @param env               目标环境（test/dev）
     * @return {@code [profitAndLossSum, ratio]}，其中 ratio 为百分比形式（乘以100后的值）
     */
    public static BigDecimal[] queryCyclicalPnl(long userId, LocalDateTime fromUtc, String collection,
                                                boolean includeBeginFloat, int type, String env) {
        BigDecimal profitAndLossSum = BigDecimal.ZERO;
        BigDecimal inUsdtSum = BigDecimal.ZERO;
        BigDecimal beginAsset;
        BigDecimal beginFloat;
        BigDecimal ratio = BigDecimal.ZERO;

        // 查合约账户 新用户仅默认开现货账户 未开户的账户大数据不统计
        List<SwapUserAssetsEntity> swapUserAssetsEntities;
        if (collection.contains("swap")) {
            swapUserAssetsEntities = UserAssetsManage.querySwapAssets(userId);
            if (swapUserAssetsEntities.size() == 0) {
                return new BigDecimal[]{profitAndLossSum, ratio};
            }
        }

        MongoCollection<Document> col = MongoUtils.getMongoConnection("superex", collection, env);
        BasicDBObject filter = new BasicDBObject("user_id", userId)
                .append("create_time", new BasicDBObject("$gte", toDate(fromUtc)));
        List<Document> result = queryMongo(col, filter, new Document("create_time", -1));

        if (result.size() >= 1) {
            Document oldest = result.get(result.size() - 1);
            beginAsset = toBigDecimal(oldest.get("total_usdt"));
            beginFloat = includeBeginFloat
                    ? toBigDecimal(oldest.get("position_usdt"))
                    : BigDecimal.ZERO;

            for (Document doc : result) {
                profitAndLossSum = profitAndLossSum.add(toBigDecimal(doc.get("profit_and_loss")));
                inUsdtSum = inUsdtSum.add(toBigDecimal(doc.get("in_usdt")));
            }
        } else {
            logger.info("用户" + userId + " MongoDB 未查询到近 " + type + " 日盈亏数据，集合：" + collection + "或用户今日创建或用户未成为热点用户；若合约业务或未开户 人工排查");
            return new BigDecimal[]{profitAndLossSum, ratio};
        }

        BigDecimal divisor = beginAsset.add(inUsdtSum).add(beginFloat);
        if (profitAndLossSum.compareTo(BigDecimal.ZERO) > 0 && divisor.compareTo(BigDecimal.ZERO) == 0) {
            ratio = BigDecimal.ONE.multiply(new BigDecimal("100"));
            return new BigDecimal[]{profitAndLossSum, ratio};
        }
        if (profitAndLossSum.compareTo(BigDecimal.ZERO) == 0 || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal[]{profitAndLossSum, ratio};
        }
        ratio = profitAndLossSum
                .divide(divisor, 20, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        logger.info("用户：" + userId + "，近" + type + "日盈亏：" + profitAndLossSum + "，近" + type + "日盈亏率：" + ratio);
        return new BigDecimal[]{profitAndLossSum, ratio};
    }
}
