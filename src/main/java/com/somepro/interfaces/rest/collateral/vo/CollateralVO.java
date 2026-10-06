package com.somepro.interfaces.rest.collateral.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当物对外对象（不可变 record）：登记/详情/清单共用。
 *
 * 每行都带 itemNo（DW-年份-序号），方便柜台跟纸面登记本对号；
 * 刻意不暴露 delFlag / createBy / updateBy 等内部字段。
 */
public record CollateralVO(Long id,
                           String itemNo,
                           Long pawnerId,
                           String category,
                           String itemName,
                           String brand,
                           String conditionLevel,
                           BigDecimal appraisedValue,
                           String status,
                           LocalDateTime createTime,
                           LocalDateTime updateTime) implements Serializable {
}
