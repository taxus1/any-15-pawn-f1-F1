package com.somepro.infrastructure.persistence.collateral;

import com.somepro.domain.collateral.repository.PawnerDirectoryPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当户名册端口的适配器（基础设施层）：实时去 t_pawner 点状态，不当物表里冗余当户信息，
 * 保证与当户模块永远对得上。
 */
@Component
public class PawnerDirectoryAdapter implements PawnerDirectoryPort {

    /** 与当户档案口径一致：注销（CLOSED）的档案不能再挂当物。 */
    private static final String STATUS_CLOSED = "CLOSED";

    private final PawnerDirectoryMapper pawnerDirectoryMapper;

    public PawnerDirectoryAdapter(PawnerDirectoryMapper pawnerDirectoryMapper) {
        this.pawnerDirectoryMapper = pawnerDirectoryMapper;
    }

    @Override
    public Mono<Boolean> isActive(Long pawnerId) {
        return blocking(() -> {
            String status = pawnerDirectoryMapper.findStatus(pawnerId);
            if (status == null) {
                return null;
            }
            return !STATUS_CLOSED.equals(status);
        });
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
