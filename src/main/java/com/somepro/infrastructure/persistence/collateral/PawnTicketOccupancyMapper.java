package com.somepro.infrastructure.persistence.collateral;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当票表只读 Mapper：当物模块只在「改估值 / 办退还」前点占用数，不做任何写入。
 * 状态口径见 doc/schema/pawn.sql：ACTIVE 在当 / REDEEMED 已赎 / FORFEITED 已绝当 / CANCELLED 已撤销。
 */
@Mapper
public interface PawnTicketOccupancyMapper {

    /** 占着这件当物的在当（ACTIVE）当票数。 */
    @Select("SELECT COUNT(*) FROM t_pawn_ticket WHERE collateral_id = #{collateralId} "
            + "AND status = 'ACTIVE' AND del_flag = 0")
    long countActiveByCollateral(@Param("collateralId") Long collateralId);
}
