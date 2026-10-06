package com.somepro.domain.collateral.repository;

import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 当物聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 当物编号（DW-年份-序号）由 {@link #insert} 的实现在写临界区内生成：
 * 命名锁全实例串行取当年最大序号 +1，item_no 唯一索引兜底，保证两件当物不会共用一个号。
 */
public interface CollateralRepository {

    /** 登记：生成全局唯一编号（DW-年份-序号）并落库，状态由聚合定为 IN_STOCK。 */
    Mono<Collateral> insert(Collateral collateral);

    /** 修改：按 id 全量更新可变字段；编号、类别、所属当户不在更新范围内。 */
    Mono<Collateral> update(Collateral collateral);

    Mono<Collateral> findById(Long id);

    Mono<Collateral> findByItemNo(String itemNo);

    /** 按条件翻清单：当户 / 类别 / 品相 / 状态随意拼；已销掉（逻辑删除）的不出。 */
    Mono<PageResult<Collateral>> page(int pageNum, int pageSize, CollateralQuery query);

    /**
     * 销掉录错的当物：逻辑删除（del_flag 置 1），清单里不再出现，账（行）留着。
     * 不存在或已销掉时抛业务异常。
     */
    Mono<Void> delete(Long id);
}
