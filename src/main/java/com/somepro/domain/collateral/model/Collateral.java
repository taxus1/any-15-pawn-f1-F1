package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 当物聚合根（纯领域对象，不带任何持久化注解）。
 *
 * 核心不变量：
 * 1. 评估价值必须是正数，写零或负数一律不收；落库前统一保留两位小数（元角分）；
 * 2. 名称、品牌、品相随时能改，不限状态；
 * 3. 评估价值的「锁」不在本聚合内部判：一件东西只要被在当当票占着，估值就不许动 ——
 *    这是跨聚合规则，由应用层查当票占用端口先确认（与当票模块对账的口径以票为准），
 *    本聚合只守「估值必须为正」这条自身规则；
 * 4. 退还不是任意状态都能走：只有在库（IN_STOCK）的东西才退得回去，
 *    已典当 / 已赎回 / 已绝当 / 已退还的都不能再退还。
 *
 * 编号 itemNo（DW-2026-0001 样式）由仓储按当年序号生成，全局唯一，业务上不可改。
 * 状态往 PAWNED / REDEEMED / FORFEITED 的迁移由当票业务驱动，不在本模块。
 */
@Getter
@Setter
public class Collateral extends BaseEntity {

    private Long id;

    /** 当物编号，如 DW-2026-0001；登记时由仓储生成，业务上不可改。 */
    private String itemNo;

    /** 所属当户 id。登记时认人（存在且未注销）由应用层查当户端口确认，落库后不改挂。 */
    private Long pawnerId;

    private CollateralCategory category;

    private String itemName;

    /** 品牌或成色说明，可空。 */
    private String brand;

    private ConditionLevel conditionLevel;

    /** 评估价值（元），必须为正数。 */
    private BigDecimal appraisedValue;

    private CollateralStatus status;

    /**
     * 工厂方法：新登记当物。品相不指定默认 GOOD，状态固定 IN_STOCK，不接受外部指定。
     * 当户是否可挂（存在且未注销）是跨聚合规则，由应用层查当户端口先确认。
     */
    public static Collateral register(Long pawnerId, CollateralCategory category, String itemName,
                                      String brand, ConditionLevel conditionLevel, BigDecimal appraisedValue) {
        Collateral collateral = new Collateral();
        if (pawnerId == null) {
            throw new BizException("所属当户不能为空");
        }
        if (category == null) {
            throw new BizException("类别不能为空");
        }
        collateral.pawnerId = pawnerId;
        collateral.category = category;
        collateral.applyProfile(itemName, brand,
                conditionLevel == null ? ConditionLevel.GOOD : conditionLevel);
        collateral.applyAppraisedValue(appraisedValue);
        collateral.status = CollateralStatus.IN_STOCK;
        return collateral;
    }

    /**
     * 修改名称 / 品牌 / 品相：这三项随时能改，不做状态限制。
     * 评估价值不在这里改 —— 改估值走 {@link #reappraise(BigDecimal)}，
     * 是否被在当当票锁死由应用层查票后决定让不让改。
     */
    public void modify(String itemName, String brand, ConditionLevel conditionLevel) {
        if (conditionLevel == null) {
            throw new BizException("品相不能为空");
        }
        applyProfile(itemName, brand, conditionLevel);
    }

    /**
     * 改评估价值。「该物正被在当当票占着，估值锁死」由应用层查当票占用端口先挡回，
     * 这里只守估值自身的规则：必须为正数。
     */
    public void reappraise(BigDecimal newValue) {
        applyAppraisedValue(newValue);
    }

    /**
     * 退还：只有在库的东西才退得回去；已典当（票还挂着）、已赎回、已绝当、已退还的都不许再走退还。
     * 「是否被在当当票占着」由应用层查当票占用端口先确认，这里守状态机自身的规则。
     * 办理时刻不落专门字段（表结构既定），由审计字段 update_time 在置 RELEASED 时记下。
     */
    public void release() {
        if (this.status != CollateralStatus.IN_STOCK) {
            throw new BizException("只有在库的当物才能退还（当前状态：" +
                    (this.status == null ? "未知" : this.status.label()) + "）");
        }
        this.status = CollateralStatus.RELEASED;
    }

    /** 公共赋值逻辑：名称必填，品牌留空按 null 存。 */
    private void applyProfile(String itemName, String brand, ConditionLevel conditionLevel) {
        if (itemName == null || itemName.isBlank()) {
            throw new BizException("当物名称不能为空");
        }
        this.itemName = itemName.trim();
        this.brand = normalize(brand);
        this.conditionLevel = conditionLevel;
    }

    /** 估值校验与归一：必须为正数（零、负数一律不收），统一两位小数。 */
    private void applyAppraisedValue(BigDecimal value) {
        if (value == null) {
            throw new BizException("评估价值不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("评估价值必须为正数，零或负数不收");
        }
        this.appraisedValue = value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
