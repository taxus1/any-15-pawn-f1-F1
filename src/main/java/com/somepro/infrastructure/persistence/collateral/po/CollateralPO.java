package com.somepro.infrastructure.persistence.collateral.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * t_collateral 表的持久化对象（PO，基础设施层）。只描述表结构，不放业务规则。
 *
 * 表已由 doc/schema/pawn.sql 建好，列名即契约，本类不做任何建表/改表动作。
 * category / condition_level / status 列直接存枚举名，由 Converter 与领域枚举互转。
 */
@Getter
@Setter
@TableName("t_collateral")
public class CollateralPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("item_no")
    private String itemNo;

    @TableField("pawner_id")
    private Long pawnerId;

    @TableField("category")
    private String category;

    @TableField("item_name")
    private String itemName;

    @TableField("brand")
    private String brand;

    @TableField("condition_level")
    private String conditionLevel;

    @TableField("appraised_value")
    private BigDecimal appraisedValue;

    @TableField("status")
    private String status;
}
