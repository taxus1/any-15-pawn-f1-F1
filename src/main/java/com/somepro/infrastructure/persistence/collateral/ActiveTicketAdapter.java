package com.somepro.infrastructure.persistence.collateral;

import com.somepro.domain.collateral.repository.ActiveTicketPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当票占用端口适配器（基础设施层）：实时去 t_pawn_ticket 点这件当物有没有在当（ACTIVE）的票。
 *
 * 结论是当物估值锁、退还门禁、销账门禁的共同依据，以当票表为权威口径，
 * 不读当物自己的 status 列（那可能因跨模块时序滞后）。
 */
@Component
public class ActiveTicketAdapter implements ActiveTicketPort {

    private final CollateralTicketQueryMapper ticketQueryMapper;

    public ActiveTicketAdapter(CollateralTicketQueryMapper ticketQueryMapper) {
        this.ticketQueryMapper = ticketQueryMapper;
    }

    @Override
    public Mono<Boolean> hasActiveTicket(Long collateralId) {
        return blocking(() -> ticketQueryMapper.existsActiveByCollateral(collateralId) > 0);
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
