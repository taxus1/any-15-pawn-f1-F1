package com.somepro.infrastructure.persistence.collateral;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当票表只读 Mapper：当物模块只问一件事 —— 这件当物眼下有没有被在当（ACTIVE）的票占着。
 * 不做任何写入。状态口径见 doc/schema/pawn.sql：ACTIVE 在当 / REDEEMED 已赎 / FORFEITED 已绝当 / CANCELLED 已撤销。
 *
 * 票结了清（REDEEMED/FORFEITED/CANCELLED）就点不到 ACTIVE，当物随之回到可再处置状态。
 */
@Mapper
public interface CollateralTicketQueryMapper {

    /** 是否存在占用该当物的在当当票（存在即 1）。 */
    @Select("SELECT EXISTS(SELECT 1 FROM t_pawn_ticket WHERE collateral_id = #{collateralId} "
            + "AND status = 'ACTIVE' AND del_flag = 0)")
    int existsActiveByCollateral(@Param("collateralId") Long collateralId);
}
