package com.somepro.application.collateral;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralCategory;
import com.somepro.domain.collateral.model.CollateralQuery;
import com.somepro.domain.collateral.model.CollateralStatus;
import com.somepro.domain.collateral.model.ConditionLevel;
import com.somepro.domain.collateral.repository.CollateralRepository;
import com.somepro.domain.collateral.repository.PawnTicketOccupancyPort;
import com.somepro.domain.collateral.repository.PawnerDirectoryPort;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 当物应用服务：编排登记、修改、详情、退还、销掉、翻清单六个用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 * 当物自身规则（名称为必填、估值为正、只有在库可退还）在 Collateral 聚合里；
 * 两条跨聚合规则在这里编排：
 * 1. 登记认人 —— 先查当户名册端口，不存在或已注销的一律挂不进来；
 * 2. 估值锁与退还锁 —— 先查当票占用端口，票还挂着在当（ACTIVE）时估值不许动、退还走不通，
 *    口径以当票表为准，与当票模块永远对得上。
 */
@Service
public class CollateralAppService {

    private final CollateralRepository collateralRepository;
    private final PawnerDirectoryPort pawnerDirectoryPort;
    private final PawnTicketOccupancyPort ticketOccupancyPort;

    public CollateralAppService(CollateralRepository collateralRepository,
                                PawnerDirectoryPort pawnerDirectoryPort,
                                PawnTicketOccupancyPort ticketOccupancyPort) {
        this.collateralRepository = collateralRepository;
        this.pawnerDirectoryPort = pawnerDirectoryPort;
        this.ticketOccupancyPort = ticketOccupancyPort;
    }

    /**
     * 登记：当户得是底账里实实在在的那一位（存在且未注销）；
     * 品相不填默认 GOOD，状态固定 IN_STOCK，编号由仓储按 DW-年份-序号 生成。
     */
    public Mono<Collateral> register(Long pawnerId, String category, String itemName, String brand,
                                     String conditionLevel, BigDecimal appraisedValue) {
        if (pawnerId == null) {
            return Mono.error(new BizException("所属当户不能为空"));
        }
        CollateralCategory cat = CollateralCategory.ofCode(category);
        ConditionLevel level = ConditionLevel.ofCode(conditionLevel);
        return pawnerDirectoryPort.isActive(pawnerId)
                .switchIfEmpty(Mono.error(new BizException("当户不存在，不能登记当物")))
                .flatMap(active -> {
                    if (!active) {
                        return Mono.error(new BizException("当户已注销，不能登记当物"));
                    }
                    Collateral collateral = Collateral.register(pawnerId, cat, itemName, brand, level, appraisedValue);
                    return collateralRepository.insert(collateral);
                });
    }

    /**
     * 修改：名称 / 品牌 / 品相随时能改，不限状态；
     * 评估价值只有「没动」或「没票占着」时才落得下去 —— 值有变化先查当票占用端口，
     * 被在当当票占着就挡回，别让放出去的当金和账面估值对不上。
     */
    public Mono<Collateral> update(Long id, String itemName, String brand,
                                   String conditionLevel, BigDecimal appraisedValue) {
        if (appraisedValue == null) {
            return Mono.error(new BizException("评估价值不能为空"));
        }
        return requireCollateral(id).flatMap(collateral -> {
            collateral.modify(itemName, brand, ConditionLevel.ofCode(conditionLevel));
            boolean valueChanging = appraisedValue.compareTo(collateral.getAppraisedValue()) != 0;
            if (!valueChanging) {
                return collateralRepository.update(collateral);
            }
            return ticketOccupancyPort.hasActiveTicket(collateral.getId()).flatMap(occupied -> {
                if (occupied) {
                    return Mono.error(new BizException("该当物正被在当当票占用，评估价值锁死，不能修改"));
                }
                collateral.reappraise(appraisedValue);
                return collateralRepository.update(collateral);
            });
        });
    }

    /** 详情：id 或 itemNo 任一指定。 */
    public Mono<Collateral> detail(Long id, String itemNo) {
        if (id != null) {
            return requireCollateral(id);
        }
        if (itemNo != null && !itemNo.isBlank()) {
            return collateralRepository.findByItemNo(itemNo.trim())
                    .switchIfEmpty(Mono.error(new BizException("当物不存在")));
        }
        return Mono.error(new BizException("请指定要查看的当物（id 或 itemNo）"));
    }

    /**
     * 退还：只有在库的东西才退得回去。先查当票占用端口 —— 票还挂着在当一律走不通；
     * 状态机自身的规则（仅 IN_STOCK 可退）由聚合守着。办理时刻由审计字段 update_time 落账。
     */
    public Mono<Collateral> release(Long id) {
        return requireCollateral(id).flatMap(collateral ->
                ticketOccupancyPort.hasActiveTicket(collateral.getId()).flatMap(occupied -> {
                    if (occupied) {
                        return Mono.error(new BizException("该当物正被在当当票占用，不能退还"));
                    }
                    collateral.release();
                    return collateralRepository.update(collateral);
                }));
    }

    /** 销掉录错的当物：逻辑删除，清单里不再出现，账留着。 */
    public Mono<Void> delete(Long id) {
        return collateralRepository.delete(id);
    }

    /** 翻清单：当户 / 类别 / 品相 / 状态随意拼，都不填翻整份；每行带当物编号。 */
    public Mono<PageResult<Collateral>> page(int pageNum, int pageSize, Long pawnerId,
                                             String category, String conditionLevel, String status) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        CollateralQuery query = CollateralQuery.of(pawnerId,
                CollateralCategory.ofCode(category),
                ConditionLevel.ofCode(conditionLevel),
                CollateralStatus.ofCode(status));
        return collateralRepository.page(pageNum, pageSize, query);
    }

    private Mono<Collateral> requireCollateral(Long id) {
        if (id == null) {
            return Mono.error(new BizException("当物 id 不能为空"));
        }
        return collateralRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("当物不存在")));
    }
}
