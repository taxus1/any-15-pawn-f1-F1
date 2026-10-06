package com.somepro.infrastructure.persistence.collateral;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.collateral.po.CollateralPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当物登记 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖常规 CRUD；编号生成需要自定义语义，用注解 SQL 写死，不建 XML。
 * 写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），见 CollateralRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface CollateralMapper extends BaseMapper<CollateralPO> {

    /**
     * 取某年全部当物编号，序号在 Java 侧取最大（只选 item_no 一列，数据量小）。
     * 不能直接 ORDER BY 序号 DESC LIMIT 1：字符串排序下 DW-2026-9999 会排在 DW-2026-10000 前面。
     *
     * ⚠️ 不过滤 del_flag：uk_item_no 唯一索引覆盖全表（含已销掉的行），
     * 序号若跳过已删行会算出已占用的号，插入撞唯一索引。
     */
    @Select("SELECT item_no FROM t_collateral WHERE item_no LIKE #{prefix}")
    List<String> findItemNosByPrefix(@Param("prefix") String prefix);
}
