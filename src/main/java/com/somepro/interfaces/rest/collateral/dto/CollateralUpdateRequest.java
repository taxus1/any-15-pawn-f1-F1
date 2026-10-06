package com.somepro.interfaces.rest.collateral.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改当物入参（用户接口层）。
 *
 * 名称/品牌/品相随时能改，任一项留空表示该项不动；评估价值在有在当当票占用时由应用层挡回（锁死）。
 * 类别、所属当户、编号不允许通过本接口改动，故这里不出现这些字段。
 */
@Getter
@Setter
public class CollateralUpdateRequest {

    private Long id;

    private String itemName;

    private String brand;

    private String conditionLevel;

    private String appraisedValue;
}
