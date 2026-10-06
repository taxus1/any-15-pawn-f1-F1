package com.somepro.infrastructure.persistence.collateral;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.collateral.po.CollateralPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当物 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖常规 CRUD；编号生成需要自定义语义（取当年编号，含已销账行），用注解 SQL 写死，不建 XML。
 * 写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），见 CollateralRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface CollateralMapper extends BaseMapper<CollateralPO> {

    /**
     * 取某年全部当物编号（序号在 Java 侧取最大，只选 item_no 一列，数据量小）。
     *
     * 刻意不带 del_flag = 0：销账（逻辑删除）只销「在清册里的可见性」，编号一经分配就永久占用，
     * 不能因为那件被销了就把它的号再发给新当物（否则同一 DW 号在账上先后指向两件东西）。
     * 不能直接 ORDER BY 字符串 DESC LIMIT 1：字符串排序下 DW-2026-9999 会排在 DW-2026-10000 前面。
     */
    @Select("SELECT item_no FROM t_collateral WHERE item_no LIKE #{prefix}")
    List<String> findItemNosByPrefix(@Param("prefix") String prefix);
}
