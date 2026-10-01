package com.novax.mapper;

import com.novax.entity.mysql.SwapTransferOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * superex_trade.transfer_order 合约子账户划转专用查询
 * transferIn  type IN (3,5,7)：现货/全币种合约/跟单 → 合约
 * transferOut type IN (4,6,8)：合约 → 现货/全币种合约/跟单
 */
public interface SwapTransferOrderMapper {

    List<SwapTransferOrderEntity> selectSwapTransferIn(@Param("userId") long userId,
                                                       @Param("updateTime") String updateTime);

    List<SwapTransferOrderEntity> selectSwapTransferOut(@Param("userId") long userId,
                                                        @Param("updateTime") String updateTime);
}
