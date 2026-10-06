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
 * 用枚举名落库（category 列直接存这些字符串），列内容可读，枚举顺序调整也不污染历史数据。
 */
public enum Category {

    JEWELRY("JEWELRY", "珠宝首饰"),
    WATCH("WATCH", "名表"),
    ELECTRONICS("ELECTRONICS", "电子产品"),
    VEHICLE("VEHICLE", "机动车"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    Category(String code, String label) {
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
     * 由外部传入值解析枚举：只认上述五个 code（大小写敏感，列里就是这么存的）。
     * 传 null 返回 null（表示「不按类别筛」）；传空串或其它写法都算非法入参，直接挡回。
     */
    public static Category ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (Category category : values()) {
            if (category.code.equals(trimmed)) {
                return category;
            }
        }
        throw new BizException("类别只支持 JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER：" + code);
    }
}
