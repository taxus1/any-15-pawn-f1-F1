package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;

/**
 * 当物类别（纯领域枚举，不依赖任何框架）。
 *
 * 只有这五个值合法，接口层传入别的写法一律不收（{@link #ofCode(String)} 抛业务异常）。
 * <ul>
 *   <li>{@link #JEWELRY} 珠宝首饰</li>
 *   <li>{@link #WATCH} 名表</li>
 *   <li>{@link #ELECTRONICS} 电子产品</li>
 *   <li>{@link #VEHICLE} 机动车</li>
 *   <li>{@link #OTHER} 其他</li>
 * </ul>
 * 用 code 落库（category 列存枚举名），而不是 ordinal：列内容可读，且枚举顺序调整不会污染历史数据。
 */
public enum CollateralCategory {

    JEWELRY("JEWELRY", "珠宝首饰"),
    WATCH("WATCH", "名表"),
    ELECTRONICS("ELECTRONICS", "电子产品"),
    VEHICLE("VEHICLE", "机动车"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    CollateralCategory(String code, String label) {
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
     * 由外部传入的类别值解析枚举：只认 JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER
     * （大小写敏感，列里就是这么存的），传 null、空串或其它写法都算非法入参。
     */
    public static CollateralCategory ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (CollateralCategory category : values()) {
            if (category.code.equals(trimmed)) {
                return category;
            }
        }
        throw new BizException("类别只支持 JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER：" + code);
    }
}
