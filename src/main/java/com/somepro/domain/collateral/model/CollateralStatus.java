package com.somepro.domain.collateral.model;

import com.somepro.common.exception.BizException;

/**
 * 当物当前状态（纯领域枚举，不依赖任何框架）。
 * <ul>
 *   <li>{@link #IN_STOCK} 在库：刚登记进来、行里代管，可改可退</li>
 *   <li>{@link #PAWNED} 已典当：被在当（ACTIVE）当票占用，估值锁死、不能退还</li>
 *   <li>{@link #REDEEMED} 已赎回：当户赎当取回</li>
 *   <li>{@link #FORFEITED} 已绝当：到期未赎、走绝当处置</li>
 *   <li>{@link #RELEASED} 已退还：未典当出去，客户直接拿回（只有在库能走这条）</li>
 * </ul>
 * 用枚举名落库（status 列直接存这些字符串）。
 *
 * PAWNED 与当票状态的实时对账不在本枚举内，由应用层查当票端口（在当票占着=已典当/锁估值/不可退）。
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
     * 由外部传入值解析枚举：只认上述五个 code（大小写敏感）。
     * 传 null 返回 null（翻清单时表示不按状态筛）；传空串或其它写法都算非法入参，直接挡回。
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
