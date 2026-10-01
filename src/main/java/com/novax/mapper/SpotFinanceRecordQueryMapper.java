package com.novax.mapper;

import com.novax.entity.mysql.SpotFinanceRecordQueryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * spot_finance_record 专项查询 Mapper（区别于实体 CRUD Mapper）
 * 用于统计合约跟单账户与现货账户之间的划转盈亏
 * operation=300：合约跟单转入至币币（转入，库中存正值）
 * operation=301：币币转出至合约跟单（转出，库中存负值）
 * operation=303：合约跟单定期分润  算作盈亏 不需要扣除
 * operation=301：合约跟单结单分润  算作盈亏 不需要扣除
 * 自己账户之间 合约跟单账户转入现货账户
 * `superex_trade`.transfer_order" 这张表不能区分出是自己跟单账户转入还是跟单员分润转入 自己转入不算做盈亏 分润转入算作盈亏
 * 故，就合约跟单业务，转入调整为查superex_trade.spot_finance_record
 */
public interface SpotFinanceRecordQueryMapper {


    List<SpotFinanceRecordQueryEntity> selectCopyTradingTransferIn(@Param("userId") long userId,
                                                                   @Param("createTime") String createTime);

    List<SpotFinanceRecordQueryEntity> selectCopyTradingTransferOut(@Param("userId") long userId,
                                                                    @Param("createTime") String createTime);
}
