package com.somepro.interfaces.rest.collateral.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 登记当物入参（用户接口层）。
 *
 * 用可变 bean + @ModelAttribute：WebFlux 下 application/x-www-form-urlencoded 表单、
 * query string 都能直接绑定。评估价值用字符串接收，由应用层解析（非数字/零/负数给明确业务提示）。
 * 品相可不传（默认 GOOD）；状态固定 IN_STOCK，不接受外部指定；编号由服务端生成。
 */
@Getter
@Setter
public class CollateralCreateRequest {

    /** 所属当户 id，必须是底账里真实、未注销的当户。 */
    private Long pawnerId;

    private String category;

    private String itemName;

    private String brand;

    private String conditionLevel;

    private String appraisedValue;
}
