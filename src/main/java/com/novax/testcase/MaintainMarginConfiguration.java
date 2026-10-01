package com.novax.testcase;

import com.novax.entity.mysql.SwapMarginEntity;
import com.novax.mapper.SwapMarginEntityMapper;
import com.novax.util.MyBatisUtil;
import org.apache.log4j.Logger;

/**
 * 查询仓位所在层级、维持保证金率、维持保证金速算额、最大持仓
 */
public class MaintainMarginConfiguration {
    static Logger logger = Logger.getLogger(MaintainMarginConfiguration.class);

    // 查询层级
    public static SwapMarginEntity queryTier(String symbol, Long position) {
        return MyBatisUtil.execute(SwapMarginEntityMapper.class, m -> m.selectTier(symbol, position));
    }

    // 币种+杠杆倍数->最大持仓量
    public static SwapMarginEntity maxPosition(String symbol, int leverage) {
        return MyBatisUtil.execute(SwapMarginEntityMapper.class, m -> m.selectPosition(symbol, leverage));
    }

}
