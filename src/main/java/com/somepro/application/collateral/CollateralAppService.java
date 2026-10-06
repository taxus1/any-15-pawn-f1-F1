package com.somepro.application.collateral;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralQuery;
import com.somepro.domain.collateral.model.CollateralStatus;
import com.somepro.domain.collateral.model.ConditionLevel;
import com.somepro.domain.collateral.repository.ActiveTicketPort;
import com.somepro.domain.collateral.repository.CollateralRepository;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.domain.pawner.repository.PawnerRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 当物应用服务：编排登记、修改、详情、退还、销账、按条件翻清单六个用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 *
 * 两条跨模块对账在这里收口：
 * 1. 登记要「认人」——挂的当户必须真实存在且未注销（跨当户聚合，查 PawnerRepository）；
 * 2. 估值锁 / 退还门禁以当票为准——改估值、退还、销账前都经 {@link ActiveTicketPort} 实时确认
 *    这件当物有没有被在当（ACTIVE）的当票占着，避免放出去的当金与账面估值对不平。
 * 编号生成、唯一约束等落库规则在仓储里；当物自身规则在 Collateral 聚合里。
 */
@Service
public class CollateralAppService {

    private final CollateralRepository collateralRepository;
    private final PawnerRepository pawnerRepository;
    private final ActiveTicketPort activeTicketPort;

    public CollateralAppService(CollateralRepository collateralRepository,
                                PawnerRepository pawnerRepository,
                                ActiveTicketPort activeTicketPort) {
        this.collateralRepository = collateralRepository;
        this.pawnerRepository = pawnerRepository;
        this.activeTicketPort = activeTicketPort;
    }

    /**
     * 登记：必须挂在真实、未注销的当户名下；评估价值必须为正；品相默认 GOOD、状态默认 IN_STOCK。
     * 编号 DW-年份-序号 由仓储生成，全局唯一。
     */
    public Mono<Collateral> register(Long pawnerId, String category, String itemName, String brand,
                                     String conditionLevel, String appraisedValue) {
        if (pawnerId == null) {
            return Mono.error(new BizException("必须指定当物挂在哪位当户名下"));
        }
        Category cat = Category.ofCode(category);
        ConditionLevel level = ConditionLevel.ofCode(blankToNull(conditionLevel));
        BigDecimal value = parseValue(appraisedValue);

        return requireActivePawner(pawnerId)
                .then(collateralRepository.insert(
                        Collateral.register(pawnerId, cat, itemName, brand, level, value)));
    }

    /**
     * 修改：名称、品牌、品相随时能改；只要还有在当当票占着，评估价值就锁死。
     * 各字段传 null/空白表示该项不动（名称除外，见聚合）。
     */
    public Mono<Collateral> update(Long id, String itemName, String brand,
                                   String conditionLevel, String appraisedValue) {
        ConditionLevel level = ConditionLevel.ofCode(blankToNull(conditionLevel));
        BigDecimal value = appraisedValue == null || appraisedValue.isBlank()
                ? null : parseValue(appraisedValue);

        return requireCollateral(id).flatMap(collateral -> activeTicketPort.hasActiveTicket(id)
                .flatMap(pawnLocked -> {
                    collateral.revise(pawnLocked, itemName, brand, level, value);
                    return collateralRepository.update(collateral);
                }));
    }

    /** 详情：id 或 itemNo（DW-编号）任一指定。 */
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
     * 退还：给没典当出去的东西准备的。只有在库、且没有在当当票占用才退得回去，
     * 已典当/已赎回/已绝当/已退还一律挡回；办理时刻由审计 update_time 落账。
     */
    public Mono<Collateral> release(Long id) {
        return requireCollateral(id).flatMap(collateral -> activeTicketPort.hasActiveTicket(id)
                .flatMap(pawnLocked -> {
                    collateral.release(pawnLocked);
                    return collateralRepository.update(collateral);
                }));
    }

    /**
     * 销掉录错的当物：逻辑删除，清单不再出现、账仍留库。
     * 还被在当当票占着的不能销（销了当票就指向一件「查无此物」的东西，账对不平）。
     */
    public Mono<Void> delete(Long id) {
        return requireCollateral(id)
                .flatMap(collateral -> activeTicketPort.hasActiveTicket(id)
                        .flatMap(pawnLocked -> {
                            if (pawnLocked) {
                                return Mono.error(new BizException(
                                        "该当物尚有在当的当票占用，不能销账；请先结清当票"));
                            }
                            return collateralRepository.deleteById(collateral.getId());
                        }));
    }

    /** 翻清单：当户/类别/品相/状态随意拼，都不填翻整份；每行带 itemNo，分页稳定走。 */
    public Mono<PageResult<Collateral>> page(int pageNum, int pageSize, Long pawnerId,
                                             String category, String conditionLevel, String status) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        CollateralQuery query = CollateralQuery.of(
                pawnerId,
                Category.ofCode(blankToNull(category)),
                ConditionLevel.ofCode(blankToNull(conditionLevel)),
                CollateralStatus.ofCode(blankToNull(status)));
        return collateralRepository.page(pageNum, pageSize, query);
    }

    /** 认人：当户必须真实存在，且不是已注销状态；冻结户仍可登记当物。 */
    private Mono<Pawner> requireActivePawner(Long pawnerId) {
        return pawnerRepository.findById(pawnerId)
                .switchIfEmpty(Mono.error(new BizException("当户不存在，不能把当物挂到一个不存在的人头上")))
                .handle((pawner, sink) -> {
                    if (pawner.getStatus() == PawnerStatus.CLOSED) {
                        sink.error(new BizException("该当户已注销，不能再往其名下登记当物"));
                    } else {
                        sink.next(pawner);
                    }
                });
    }

    private Mono<Collateral> requireCollateral(Long id) {
        if (id == null) {
            return Mono.error(new BizException("必须指定当物 id"));
        }
        return collateralRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("当物不存在")));
    }

    /** 评估价值入参解析：必须是正数金额；空串交给上游按「不传」处理（故这里只收非空）。 */
    private BigDecimal parseValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("评估价值不能为空");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new BizException("评估价值必须是正数金额：" + raw);
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("评估价值必须是正数，零和负数一律不收");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
