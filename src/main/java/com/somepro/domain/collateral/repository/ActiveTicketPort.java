package com.somepro.domain.collateral.repository;

import reactor.core.publisher.Mono;

/**
 * 当票占用端口：当物模块要向当票模块要的一个事实 —— 这件当物眼下有没有被「在当（ACTIVE）」的当票占着。
 *
 * 这是对账的命门：票还挂着在当，当物就是已典当、估值锁死、退还走不通；票结了清才回到可再处置状态。
 * 当物自己的 status 列可能因跨模块时序滞后，这里每次都实时去当票表点，以当票为准，
 * 避免柜台放出的当金与账面估值对不平。
 */
public interface ActiveTicketPort {

    /**
     * 该当物是否存在在当（ACTIVE、未删除）的当票。
     */
    Mono<Boolean> hasActiveTicket(Long collateralId);
}
