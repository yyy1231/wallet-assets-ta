package com.novax.testcase;

import com.novax.util.MyBatisUtil;
import com.novax.util.PropertyUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.testng.annotations.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 所有测试类的 TestNG 基类，负责初始化环境配置、数据库连接、日志上下文。
 */
public class BaseTest {
    public static Logger logger = Logger.getLogger(BaseTest.class);
    public static volatile String TEST_ENV;
    public static volatile String DEV_ENV;
    public String currentEnv;

    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 日期格式化器
    protected static final LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);                    // 今日零点（本地时间）
    protected static final LocalDateTime todayUtc = today.minusHours(8).truncatedTo(ChronoUnit.HOURS);               // 今日零点对应的 UTC 时间
    protected static final LocalDateTime sevenUtc = today.minusDays(6).minusHours(8).truncatedTo(ChronoUnit.HOURS);  // 近 7 天起始 UTC 时间
    protected static final LocalDateTime thirtyUtc = today.minusDays(31).minusHours(8).truncatedTo(ChronoUnit.HOURS); // 近 31 天起始 UTC 时间

    protected static final BigDecimal COIN_TOLERANT  = new BigDecimal("0.0001"); // 本币误差（币种数量精度）
    // protected static final BigDecimal COIN_TOLERANT = BigDecimal.ZERO;
    protected static final BigDecimal USDT_TOLERANT  = BigDecimal.valueOf(2);           // 折U误差（10 USDT）
    // protected static final BigDecimal USDT_TOLERANT = BigDecimal.ZERO;
    protected static final BigDecimal BTC_TOLERANT   = new BigDecimal("0.001");  // 折Btc误差
    // protected static final BigDecimal BTC_TOLERANT = BigDecimal.ZERO;
    protected static final BigDecimal RATIO_TOLERANT = new BigDecimal("0.05");   // 盈亏率误差
    // protected static final BigDecimal RATIO_TOLERANT = BigDecimal.ZERO;
    protected static final BigDecimal PNL_TOLERANT   = new BigDecimal("0.5");           // 单笔盈亏误差
    // protected static final BigDecimal PNL_TOLERANT = BigDecimal.ZERO;

    /**
     * Suite 级别初始化（@BeforeSuite）：加载配置文件、初始化 MyBatis 数据源、组装各服务基础 URL。
     *
     * @param testEnv 测试环境名称，默认 "test"
     * @param devEnv  开发环境名称，默认 "dev"
     */
    @Parameters({"testEnv", "devEnv"})
    @BeforeSuite(alwaysRun = true)
    public void globalSetup(@Optional("test") String testEnv, @Optional("dev") String devEnv) {
        TEST_ENV = testEnv;
        DEV_ENV = devEnv;

        PropertyUtil.init(TEST_ENV);
        PropertyUtil.init(DEV_ENV);
        MyBatisUtil.init(TEST_ENV);
        MyBatisUtil.init(DEV_ENV);

        logger.info(Thread.currentThread().getName() + " >>> 资产管理环境准备就绪 | test:" + TEST_ENV + " | dev:" + DEV_ENV);
    }

    /**
     * Class 级别初始化（@BeforeClass）：设置当前测试类使用的环境，并切换 MyBatis 数据源。
     *
     * @param currentEnv 当前类使用的环境名称，默认 "test"
     */
    @Parameters({"currentEnv"})
    @BeforeClass(alwaysRun = true)
    public void classSetup(@Optional("test") String currentEnv) {
        this.currentEnv = currentEnv;
        MyBatisUtil.setCurrentEnv(currentEnv);
        logger.info(Thread.currentThread().getName() + " >>> [BeforeClass] 资产测试类就绪 | 当前环境：" + this.currentEnv);
    }

    /**
     * Method 级别前置处理（@BeforeMethod）：将用例名写入 MDC 日志上下文，并确保 MyBatis 使用正确的环境。
     *
     * @param method   当前执行的测试方法
     * @param testData 测试方法的参数数据
     */
    @BeforeMethod(alwaysRun = true)
    public void setLogContext(Method method, Object[] testData) {
        String caseName = method.getName() + "_" + Thread.currentThread().getId();
        MDC.put("caseID", caseName);
        MyBatisUtil.setCurrentEnv(this.currentEnv);
    }

    /**
     * Method 级别后置处理（@AfterMethod）：清除 MDC 日志上下文，避免日志上下文泄漏到其他线程。
     */
    @AfterMethod(alwaysRun = true)
    public void clearLogContext() {
        MDC.remove("caseID");
    }

    /**
     * alias 或原始 ID → userId，委托 TestUserConfig 按当前环境解析
     */
    protected long resolveUserId(String aliasOrId) {
        return TestUserConfig.resolve(aliasOrId, currentEnv);
    }
}
