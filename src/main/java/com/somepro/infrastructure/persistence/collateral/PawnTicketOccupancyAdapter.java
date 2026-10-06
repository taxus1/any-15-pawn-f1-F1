package com.somepro.infrastructure.persistence.collateral;

import com.somepro.domain.collateral.repository.PawnTicketOccupancyPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当票占用端口的适配器（基础设施层）：实时去 t_pawn_ticket 点在当票数，
 * 不在当物表里冗余占用标记 —— 口径以当票为准，两处才不会串。
 */
@Component
public class PawnTicketOccupancyAdapter implements PawnTicketOccupancyPort {

    private final PawnTicketOccupancyMapper pawnTicketOccupancyMapper;

    public PawnTicketOccupancyAdapter(PawnTicketOccupancyMapper pawnTicketOccupancyMapper) {
        this.pawnTicketOccupancyMapper = pawnTicketOccupancyMapper;
    }

    @Override
    public Mono<Boolean> hasActiveTicket(Long collateralId) {
        return blocking(() -> pawnTicketOccupancyMapper.countActiveByCollateral(collateralId) > 0);
    }

    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
