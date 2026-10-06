package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;

/**
 * 当物品相（纯领域枚举，不依赖任何框架）。
 *
 * 四档，新登记当物默认 {@link #GOOD} 良好：
 * <ul>
 *   <li>{@link #NEW} 全新</li>
 *   <li>{@link #GOOD} 良好（登记默认）</li>
 *   <li>{@link #FAIR} 一般</li>
 *   <li>{@link #POOR} 较差</li>
 * </ul>
 * 用枚举名落库（condition_level 列直接存这些字符串）。
 */
public enum ConditionLevel {

    NEW("NEW", "全新"),
    GOOD("GOOD", "良好"),
    FAIR("FAIR", "一般"),
    POOR("POOR", "较差");

    private final String code;
    private final String label;

    ConditionLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 由外部传入值解析枚举：只认 NEW / GOOD / FAIR / POOR（大小写敏感）。
     * 传 null 返回 null（登记时由聚合补成默认 GOOD、修改时表示品相不变、查询时表示不按品相约）；
     * 传空串或其它写法都算非法入参，直接挡回。
     */
    public static ConditionLevel ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (ConditionLevel level : values()) {
            if (level.code.equals(trimmed)) {
                return level;
            }
        }
        throw new BizException("品相只支持 NEW / GOOD / FAIR / POOR：" + code);
    }
}
