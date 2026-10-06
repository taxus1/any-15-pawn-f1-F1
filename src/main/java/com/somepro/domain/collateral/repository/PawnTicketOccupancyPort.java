package com.somepro.domain.collateral.repository;

import reactor.core.publisher.Mono;

/**
 * 当票占用端口：当物模块改估值、办退还前要对账的一件事——这件东西有没有被在当的票占着。
 * （领域层定义，基础设施层跨表实现，只读 t_pawn_ticket，不写。）
 *
 * 口径以当票为准而不是以当物自身状态为准：票还挂着在当（ACTIVE），这件就是估值锁死、退还走不通；
 * 票结了清（已赎/已绝当/已撤销），东西才回到可以再处置的状态。两处口径串了，
 * 柜台放出去的钱和账面估值就永远对不平。
 */
public interface PawnTicketOccupancyPort {

    /** 该当物是否被在当（ACTIVE）的当票占着。 */
    Mono<Boolean> hasActiveTicket(Long collateralId);
}
