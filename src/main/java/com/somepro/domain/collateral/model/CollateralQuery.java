package com.somepro.domain.collateral.model;

/**
 * 当物清单查询条件（不可变值对象）。
 *
 * - pawnerId 精确匹配某位当户名下；category / conditionLevel / status 精确匹配；
 * - 任一项为 null 即不参与过滤，四个条件随意拼都能查，都不填翻整份清单；
 * - 已销掉（逻辑删除）的当物由仓储层 @TableLogic 自动挡在清单外，这里不需要条件。
 */
public record CollateralQuery(Long pawnerId,
                              CollateralCategory category,
                              ConditionLevel conditionLevel,
                              CollateralStatus status) {

    public static CollateralQuery of(Long pawnerId, CollateralCategory category,
                                     ConditionLevel conditionLevel, CollateralStatus status) {
        return new CollateralQuery(pawnerId, category, conditionLevel, status);
    }
}
