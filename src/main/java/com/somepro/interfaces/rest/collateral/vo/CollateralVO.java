package com.somepro.interfaces.rest.collateral.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当物清单/详情对外对象（不可变 record）。
 *
 * 每行都带 itemNo，方便柜台跟纸面登记本对号。
 * updateTime 一并带出：退还的办理时刻就落在它上面（表结构既定，不另设退还时刻列），
 * 已退还的当物看这个时间就是退办时刻。
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
