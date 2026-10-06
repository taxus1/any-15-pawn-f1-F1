package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;

/**
 * 当物状态（纯领域枚举，不依赖任何框架）。
 *
 * 只有这五个值合法，接口层传入别的写法一律不收（{@link #ofCode(String)} 抛业务异常）。
 * <ul>
 *   <li>{@link #IN_STOCK} 在库（新登记的初始状态）</li>
 *   <li>{@link #PAWNED} 已典当（被在当当票占着）</li>
 *   <li>{@link #REDEEMED} 已赎回</li>
 *   <li>{@link #FORFEITED} 已绝当</li>
 *   <li>{@link #RELEASED} 已退还（客户直接把东西拿回去）</li>
 * </ul>
 * PAWNED / REDEEMED / FORFEITED 的迁移由当票业务驱动；本模块只负责登记（→IN_STOCK）与退还（→RELEASED）。
 * 用 code 落库（status 列存枚举名），而不是 ordinal：列内容可读，且枚举顺序调整不会污染历史数据。
 */
public enum CollateralStatus {

    IN_STOCK("IN_STOCK", "在库"),
    PAWNED("PAWNED", "已典当"),
    REDEEMED("REDEEMED", "已赎回"),
    FORFEITED("FORFEITED", "已绝当"),
    RELEASED("RELEASED", "已退还");

    private final String code;
    private final String label;

    CollateralStatus(String code, String label) {
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
     * 由外部传入的状态值解析枚举：只认 IN_STOCK / PAWNED / REDEEMED / FORFEITED / RELEASED
     * （大小写敏感，列里就是这么存的），传 null、空串或其它写法都算非法入参。
     */
    public static CollateralStatus ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (CollateralStatus status : values()) {
            if (status.code.equals(trimmed)) {
                return status;
            }
        }
        throw new BizException("状态只支持 IN_STOCK / PAWNED / REDEEMED / FORFEITED / RELEASED：" + code);
    }
}
