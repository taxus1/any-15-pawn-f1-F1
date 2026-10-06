package com.somepro.interfaces.rest.collateral.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 按 id 操作当物的入参（退还 / 销账）。POST 表单体在 WebFlux 下需经 @ModelAttribute 绑定。
 */
@Getter
@Setter
public class CollateralIdRequest {

    private Long id;
}
