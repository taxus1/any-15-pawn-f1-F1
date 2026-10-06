package com.somepro.interfaces.rest.collateral.converter;

import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.collateral.vo.CollateralVO;
import com.somepro.interfaces.rest.common.vo.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当物领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class CollateralVoConverter {

    private CollateralVoConverter() {
    }

    public static CollateralVO toVo(Collateral domain) {
        return new CollateralVO(
                domain.getId(),
                domain.getItemNo(),
                domain.getPawnerId(),
                domain.getCategory() == null ? null : domain.getCategory().code(),
                domain.getItemName(),
                domain.getBrand(),
                domain.getConditionLevel() == null ? null : domain.getConditionLevel().code(),
                domain.getAppraisedValue(),
                domain.getStatus() == null ? null : domain.getStatus().code(),
                domain.getCreateTime(),
                domain.getUpdateTime());
    }

    public static PageVO<CollateralVO> toPageVo(PageResult<Collateral> page) {
        List<CollateralVO> content = page.content().stream()
                .map(CollateralVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
