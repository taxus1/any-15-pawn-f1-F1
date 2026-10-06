package com.somepro.infrastructure.persistence.collateral.converter;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralStatus;
import com.somepro.domain.collateral.model.ConditionLevel;
import com.somepro.infrastructure.persistence.collateral.po.CollateralPO;

/**
 * CollateralPO（表）↔ Collateral（领域）转换器（基础设施层），PO 不外泄。
 * category / condition_level / status 三列都存枚举名，读出时 valueOf 还原；
 * 库里的值受写入端约束，必为各枚举的合法值之一。
 */
public final class CollateralPoConverter {

    private CollateralPoConverter() {
    }

    public static CollateralPO toPo(Collateral domain) {
        CollateralPO po = new CollateralPO();
        po.setId(domain.getId());
        po.setItemNo(domain.getItemNo());
        po.setPawnerId(domain.getPawnerId());
        po.setCategory(domain.getCategory() == null ? null : domain.getCategory().code());
        po.setItemName(domain.getItemName());
        po.setBrand(domain.getBrand());
        po.setConditionLevel(domain.getConditionLevel() == null ? null : domain.getConditionLevel().code());
        po.setAppraisedValue(domain.getAppraisedValue());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().code());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static Collateral toDomain(CollateralPO po) {
        Collateral domain = new Collateral();
        domain.setId(po.getId());
        domain.setItemNo(po.getItemNo());
        domain.setPawnerId(po.getPawnerId());
        domain.setCategory(po.getCategory() == null ? null : Category.valueOf(po.getCategory()));
        domain.setItemName(po.getItemName());
        domain.setBrand(po.getBrand());
        domain.setConditionLevel(po.getConditionLevel() == null
                ? null : ConditionLevel.valueOf(po.getConditionLevel()));
        domain.setAppraisedValue(po.getAppraisedValue());
        domain.setStatus(po.getStatus() == null ? null : CollateralStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
