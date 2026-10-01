package com.novax.testcase;

import com.novax.entity.mysql.*;
import com.novax.mapper.*;
import com.novax.util.MyBatisUtil;
import com.novax.util.RandomChar;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class GenerateUserTest extends BaseTest {
    static Logger logger = Logger.getLogger(GenerateUserTest.class);

    @Test(description = "批量创建测试交易员用户")
    public static void createTraders() {
        int n = 2;
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            String email = "walletAssets" + random.nextInt(100) + RandomChar.getRandomChar(3) + "@163.com";
            createUser(email);
            Long userId = queryByEmail(email).getId();
            traderReady(userId, false);
        }
    }

    public static void createUser(String email) {
        logger.info("测试sql执行时间,start:" + new Date());
        MyBatisUtil.executeVoid(UserEntityMapper.class, mapper -> {
            UserEntity userEntity = UserEntity.builder()
                    .tenantId(1L)
                    .areaCode("")
                    .phone(null)
                    .email(email)
                    .password("f2cf8b8a01fe9cb74208702dc87b1235")
                    .salt("60cde9c442f12e5ad6312dfc38bb394e")
                    .nickname("")
                    .headUrl("/static/images/userhead/u004.jpg")
                    .trueName(null)
                    .nationality("")
                    .idCardType(1)
                    .idCard("")
                    .authLevel(0)
                    .isAuth(0)
                    .assistantCurrency("usd")
                    .language("en")
                    .feeFlag(1)
                    .inviteCode(null)
                    .inviterId(20222039L)
                    .status(1)
                    .level(0)
                    .levelUpdateTime(new Date())
                    .spotDiscount(0)
                    .swapDiscount(1)
                    .swapCurrencyDiscount(0)
                    .googleKey(null)
                    .lastModified(null)
                    .isMerchant(0)
                    .isSwapOpened(1)
                    .isAnswerPassed(1)
                    .swapStatus(1)
                    .isLimitSpot(0)
                    .isLimitWithdrawal(0)
                    .limitMarket(null)
                    .isDeleted(0)
                    .isStatistics(1)
                    .isWhitelist(0)
                    .type(0)
                    .registerClient(1)
                    .registerIp("0.0.0.0")
                    .region(null)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            int insert = mapper.insert(userEntity);
            logger.info("测试sql执行时间,END：" + new Date());
            Assert.assertEquals(insert, 1, "user create fail");
        });
    }

    public static void traderReady(Long userId, Boolean assetsConfig) {
        logger.info("***************TRADER READY BEGIN***************");
        // 1、开通合约、答题
        boolean isSwapOpened = isSwapOpened(userId);
        if (!isSwapOpened) {
            openSwap(userId);
        }
        // 2、配置仓位类型
        boolean modeConfigured = isModeConfigured(userId);
        if (!modeConfigured) {
            configureMode(userId);
        }
        // 是否初始化资产
        if (!assetsConfig) {
            return;
        }
        // 3、有合约账户-USDT否，无则创建，初始化资产10000
        boolean haveSwapAccount = isHaveSwapAccount(userId, "usdt");
        if (!haveSwapAccount) {
            createSwapAccount(userId, "usdt");
        }
        // 4、用户合约杠杆倍数 默认是150 修改为10
        initialUserLeverage(userId, "btc_usdt");
        initialUserLeverage(userId, "eth_usdt");

        // 初始化表vip_level_up
        // UserInfo.initVipLevelUp(userId);

        // 初始化资产
        createSpotAccount(userId, "usdt", BigDecimal.valueOf(100000));
        createSpotAccount(userId, "btc", BigDecimal.valueOf(1));
        createSpotAccount(userId, "eth", BigDecimal.valueOf(10));
        createSpotAccount(userId, "aisi", BigDecimal.valueOf(1000));
        updateSpotAccount(userId, "et", BigDecimal.valueOf(100));
        logger.info("***************TRADER READY END***************");
    }

    // 通过邮箱查userId
    public static UserEntity queryByEmail(String email) {
        return MyBatisUtil.execute(UserEntityMapper.class, m -> m.selectByEmail(email));
    }

    // 开通合约、答题
    public static void openSwap(Long userId) {
        MyBatisUtil.executeVoid(UserEntityMapper.class, mapper -> {
            UserEntity user = mapper.selectByPrimaryKey(userId);
            if (user.getIsSwapOpened() == 0 || user.getIsAnswerPassed() == 0) {
                user.setIsSwapOpened(1);
                user.setIsAnswerPassed(1);
                int result = mapper.updateByPrimaryKey(user);
                Assert.assertEquals(result, 1, "user信息更新失败");
            } else {
                logger.info("用户：" + userId + "已开通合约账户");
            }
        });
    }

    // 配置仓位模式 --默认全仓1
    public static void configureMode(Long userId) {
        MyBatisUtil.executeVoid(SwapFrontUserConfigEntityMapper.class, mapper -> {
            SwapFrontUserConfigEntity configEntity = mapper.selectByUserId(userId);
            if (configEntity == null) {
                SwapFrontUserConfigEntity record = SwapFrontUserConfigEntity.builder()
                        .tenantId(1L)
                        .userId(userId)
                        .mode(1)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
                int num = mapper.insert(record);
                Assert.assertEquals(num, 1, "swap_front_user_config数据插入失败");
            } else {
                logger.info("用户：" + userId + "已有用户仓位配置");
            }
        });
    }

    // 是否已开通合约
    public static boolean isSwapOpened(Long userId) {
        UserEntity user = MyBatisUtil.execute(UserEntityMapper.class, m -> m.selectByPrimaryKey(userId));
        if (user.getIsSwapOpened() == 1 && user.getIsAnswerPassed() == 1) {
            logger.info("用户：" + userId + "已开通合约业务");
            return true;
        } else {
            logger.info("用户：" + userId + "未开通合约业务");
            return false;
        }
    }

    // 是否已配置仓位模式mode
    public static boolean isModeConfigured(Long userId) {
        SwapFrontUserConfigEntity configEntity = MyBatisUtil.execute(SwapFrontUserConfigEntityMapper.class, m -> m.selectByUserId(userId));
        if (configEntity == null) {
            logger.info("用户：" + userId + "没有仓位模式配置信息");
            return false;
        } else {
            logger.info("用户：" + userId + "有仓位模式配置信息");
            return true;
        }
    }

    // 是否有合约账户-USDT
    public static boolean isHaveSwapAccount(Long userId, String currency) {
        SwapFrontUserAssetsEntity entity = MyBatisUtil.execute(SwapFrontUserAssetsEntityMapper.class, m -> m.selectByUserId(userId, currency));
        if (entity == null) {
            logger.info("用户" + userId + "合约账户无USDT资产");
            return false;
        }
        return true;
    }

    // 创建现货账户
    public static void createSpotAccount(Long userId, String currency, BigDecimal amount) {
        MyBatisUtil.executeVoid(SpotUserAssetsEntityMapper.class, mapper -> {
            SpotUserAssetsEntity spotUserAssetsEntity = mapper.selectByUserId(userId, currency, amount);
            if (spotUserAssetsEntity == null) {
                SpotUserAssetsEntity assetsEntity = SpotUserAssetsEntity.builder()
                        .tenantId(1L)
                        .userId(userId)
                        .currency(currency)
                        .available(amount)
                        .frozen(BigDecimal.ZERO)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
                int insert = mapper.insert(assetsEntity);
                Assert.assertEquals(insert, 1, "用户：" + userId + "现货账户创建失败");
            }
        });
    }

    // 更新现货账户资产
    public static void updateSpotAccount(Long userId, String currency, BigDecimal amount) {
        MyBatisUtil.executeVoid(SpotUserAssetsEntityMapper.class, mapper -> {
            List<SpotUserAssetsEntity> spotUserAssetsEntities = mapper.selectByCurrency(userId, currency);
            if (spotUserAssetsEntities.size() == 0) {
                createSpotAccount(userId, currency, amount);
            } else {
                SpotUserAssetsEntity assetsEntity = spotUserAssetsEntities.get(0);
                assetsEntity.setAvailable(amount);
                int i = mapper.updateByPrimaryKey(assetsEntity);
                Assert.assertEquals(i, 1, "用户：" + userId + "现货账户余额更新失败");
            }
        });
    }

    // 创建合约账户
    public static void createSwapAccount(Long userId, String currency) {
        MyBatisUtil.executeVoid(SwapFrontUserAssetsEntityMapper.class, mapper -> {
            SwapFrontUserAssetsEntity existing = mapper.selectByUserId(userId, currency);
            if (existing == null) {
                SwapFrontUserAssetsEntity assetsEntity = SwapFrontUserAssetsEntity.builder()
                        .tenantId(1L)
                        .userId(userId)
                        .symbol("usdt")
                        .currency(currency)
                        .available(BigDecimal.valueOf(10000))
                        .frozen(BigDecimal.ZERO)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
                int insert = mapper.insert(assetsEntity);
                Assert.assertEquals(insert, 1, "用户：" + userId + "合约账户创建失败");
            }
        });
    }

    // 设置用户杠杆倍数
    public static void initialUserLeverage(Long userId, String symbol) {
        MyBatisUtil.executeVoid(SwapFrontUserLeverageEntityMapper.class, mapper -> {
            SwapFrontUserLeverageEntity entity = SwapFrontUserLeverageEntity.builder()
                    .tenantId(1L)
                    .symbol(symbol)
                    .userId(userId)
                    .crossedLeverage(10)
                    .fissionLeverage(10)
                    .fixedLeverage(10)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            int insert = mapper.insert(entity);
            Assert.assertEquals(insert, 1, "user leverage initial fail");
        });
    }
}
