package com.somepro.interfaces.rest.collateral.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 修改当物入参（用户接口层）。
 *
 * 字段按全量传：名称 / 品牌 / 品相随时能改；评估价值若与账面不同，
 * 需该物没被在当当票占着才落得下去（被占着时估值锁死，由应用层挡回）。
 * 编号、类别、所属当户不在可改范围内。
 */
@Getter
@Setter
public class CollateralUpdateRequest {

    private Long id;

    private String itemName;

    private String brand;

    private String conditionLevel;

    private BigDecimal appraisedValue;
}
