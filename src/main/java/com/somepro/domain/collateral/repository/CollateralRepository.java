package com.somepro.domain.collateral.repository;

import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 当物聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 编号全局唯一（DW-年份-序号，两件不共用一号）由 {@link #insert} 的实现保证：
 * 写入临界区由 MySQL 命名锁在全实例串行化，锁内取当年最大序号 +1，撞号再整段重试。
 * 逻辑删除（销账不留痕于清单、行仍在库里）由 @TableLogic 兜底，见 {@link #deleteById}。
 */
public interface CollateralRepository {

    /**
     * 新登记：分配雪花 id、生成全局唯一编号（DW-年份-序号）并落库。
     * 返回回填编号与审计字段后的领域对象。
     */
    Mono<Collateral> insert(Collateral collateral);

    /**
     * 修改：编号永不改、所属当户永不改，只更新名称/品牌/品相/估值等可变字段。
     * 目标不存在（含已销掉）时抛业务异常。
     */
    Mono<Collateral> update(Collateral collateral);

    Mono<Collateral> findById(Long id);

    Mono<Collateral> findByItemNo(String itemNo);

    /** 按条件翻清单；逻辑删除的不出现，稳定按 id 升序分页。 */
    Mono<PageResult<Collateral>> page(int pageNum, int pageSize, CollateralQuery query);

    /** 销掉录错的当物：逻辑删除（del_flag 置 1），清单不再出现、账仍留库。 */
    Mono<Void> deleteById(Long id);
}
