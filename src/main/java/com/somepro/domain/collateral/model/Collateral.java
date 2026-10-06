package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 当物聚合根（纯领域对象，不带任何持久化注解）。
 *
 * 一条记录 = 一件当物。核心不变量：
 * 1. 必须挂在一个真实、未注销的当户名下 —— 对当户存在性/状态的跨聚合校验在应用层做
 *    （当物登记要「认人」，不能挂到不存在或已注销的人头上）；
 * 2. 类别只认 {@link Category} 五值、品相只认 {@link ConditionLevel} 四值，别的写法在枚举解析阶段就被挡回；
 * 3. 评估价值必须是正数，零、负数、null 一律不收；
 * 4. 新登记默认品相 GOOD、状态 IN_STOCK（在库）；
 * 5. 名称、品牌、品相随时能改；但只要这件东西还被在当（ACTIVE）的当票占着，
 *    评估价值就锁死 —— 别让放出去的当金和账面估值对不上；
 * 6. 退还是给没典当出去的东西准备的：只有在库（IN_STOCK）、且没有在当当票占用时才退得回去。
 *
 * 编号 itemNo（DW-2026-0001 样式）由仓储按当年序号生成，全局唯一、两件当物不共用一号。
 * 状态与当票的实时对账（是否被在当票占着）由应用层查当票端口，结论以布尔 pawnLocked 传入。
 */
@Getter
@Setter
public class Collateral extends BaseEntity {

    private Long id;

    /** 当物编号，如 DW-2026-0001；登记时由仓储生成，业务上不可改。 */
    private String itemNo;

    /** 所属当户 id（t_pawner.id）。 */
    private Long pawnerId;

    private Category category;

    private String itemName;

    /** 品牌或成色说明，可空。 */
    private String brand;

    private ConditionLevel conditionLevel;

    /** 评估价值（元），正数。 */
    private BigDecimal appraisedValue;

    private CollateralStatus status;

    /**
     * 工厂方法：登记新当物。默认品相 GOOD、状态 IN_STOCK（在库）。
     *
     * @param conditionLevel 品相，外部不传（null）时补成默认 GOOD
     */
    public static Collateral register(Long pawnerId, Category category, String itemName, String brand,
                                      ConditionLevel conditionLevel, BigDecimal appraisedValue) {
        if (pawnerId == null) {
            throw new BizException("必须指定当物挂在哪位当户名下");
        }
        if (category == null) {
            throw new BizException("类别不能为空");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new BizException("当物名称不能为空");
        }
        requirePositive(appraisedValue);

        Collateral collateral = new Collateral();
        collateral.pawnerId = pawnerId;
        collateral.category = category;
        collateral.itemName = itemName.trim();
        collateral.brand = normalize(brand);
        collateral.conditionLevel = conditionLevel == null ? ConditionLevel.GOOD : conditionLevel;
        collateral.appraisedValue = appraisedValue;
        collateral.status = CollateralStatus.IN_STOCK;
        return collateral;
    }

    /**
     * 修改登记信息。名称/品牌/品相随时能改；评估价值只在没有被在当当票占用时能改。
     *
     * @param pawnLocked 是否被在当（ACTIVE）当票占着 —— 由应用层查当票端口得出，是估值锁的权威口径
     * @param itemName   非空则改名称，null/空白表示名称不动
     * @param brand      品牌/成色说明（空白归一化为 null）；null 表示不动
     * @param level      品相；null 表示品相不动
     * @param value      新评估价值；null 表示本次不动估值，非 null 必须为正数且估值未锁
     */
    public void revise(boolean pawnLocked, String itemName, String brand,
                       ConditionLevel level, BigDecimal value) {
        if (itemName != null && !itemName.isBlank()) {
            this.itemName = itemName.trim();
        }
        if (brand != null) {
            this.brand = normalize(brand);
        }
        if (level != null) {
            this.conditionLevel = level;
        }
        if (value != null) {
            requirePositive(value);
            if (pawnLocked || this.status == CollateralStatus.PAWNED) {
                throw new BizException("该当物尚有在当的当票占用，评估价值已锁死，不能调整");
            }
            this.appraisedValue = value;
        }
    }

    /**
     * 退还：客户把没典当出去的东西直接拿回。只有在库、且没有在当当票占用才退得回去；
     * 已典当/已赎回/已绝当/已退还都不能再走退还。办理时刻由审计字段 update_time 落账。
     *
     * @param pawnLocked 是否被在当（ACTIVE）当票占着 —— 以当票表实时口径为准
     */
    public void release(boolean pawnLocked) {
        if (pawnLocked) {
            throw new BizException("该当物尚有在当的当票占用，不能退还；请先结清当票");
        }
        if (this.status != CollateralStatus.IN_STOCK) {
            throw new BizException("只有在库的当物才退得回去，当前状态："
                    + (this.status == null ? "-" : this.status.label()));
        }
        this.status = CollateralStatus.RELEASED;
    }

    public boolean isInStock() {
        return status == CollateralStatus.IN_STOCK;
    }

    /** 评估价值必须为正数：null、零、负数一律不收。 */
    private static void requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("评估价值必须是正数，零和负数一律不收");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
