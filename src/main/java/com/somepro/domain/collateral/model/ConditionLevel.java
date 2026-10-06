package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;

/**
 * 当物品相（纯领域枚举，不依赖任何框架）。
 *
 * 只有这四档合法，接口层传入别的写法一律不收（{@link #ofCode(String)} 抛业务异常）。
 * <ul>
 *   <li>{@link #NEW} 全新</li>
 *   <li>{@link #GOOD} 良好（新登记不指定品相时的默认档）</li>
 *   <li>{@link #FAIR} 一般</li>
 *   <li>{@link #POOR} 较差</li>
 * </ul>
 * 用 code 落库（condition_level 列存枚举名），而不是 ordinal：列内容可读，且枚举顺序调整不会污染历史数据。
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
     * 由外部传入的品相值解析枚举：只认 NEW / GOOD / FAIR / POOR（大小写敏感，列里就是这么存的），
     * 传 null、空串或其它写法都算非法入参。
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
