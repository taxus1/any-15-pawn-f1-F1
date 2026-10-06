package com.somepro.interfaces.rest.collateral.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 登记当物入参（用户接口层）。
 *
 * 用可变 bean + @ModelAttribute：Spring WebFlux 下 application/x-www-form-urlencoded 表单、
 * query string 都能直接绑定（@RequestParam 在 WebFlux 不解析表单体）。
 * 状态不接受外部传入，新登记固定 IN_STOCK；品相不填默认 GOOD。
 */
@Getter
@Setter
public class CollateralCreateRequest {

    private Long pawnerId;

    private String category;

    private String itemName;

    private String brand;

    private String conditionLevel;

    private BigDecimal appraisedValue;
}
