package com.somepro.domain.collateral.model;

/**
 * 当物清单查询条件（不可变值对象）。
 *
 * 当户（pawnerId）、类别、品相、状态随意拼，任一项为 null 即不参与过滤；全 null 翻整份清册。
 * 类别/品相/状态在进入本对象前由 {@code ofCode} 解析过，非法写法已在解析阶段挡回。
 */
public record CollateralQuery(Long pawnerId,
                              Category category,
                              ConditionLevel conditionLevel,
                              CollateralStatus status) {

    public static CollateralQuery of(Long pawnerId, Category category,
                                     ConditionLevel conditionLevel, CollateralStatus status) {
        return new CollateralQuery(pawnerId, category, conditionLevel, status);
    }
}
