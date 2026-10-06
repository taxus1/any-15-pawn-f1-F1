package com.somepro.infrastructure.persistence.collateral;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.CollateralQuery;
import com.somepro.domain.collateral.repository.CollateralRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.collateral.converter.CollateralPoConverter;
import com.somepro.infrastructure.persistence.collateral.po.CollateralPO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 当物仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 blocking(...) 桥接进响应式链路。
 *
 * 关键业务语义 —— 编号生成 DW-yyyy-NNNN（含并发、含销账不复用）：
 * item_no 有全局唯一索引，但「当年序号取最大 +1」必须串行，否则并发登记会算出同一个号。
 * 登记先抢 MySQL 命名锁 GET_LOCK('collateral:write')（全实例互斥），锁内取号 + 写入。
 * 取号刻意包含已销账（del_flag=1）的行：编号一经分配永久占用，销掉的号也不发给新当物。
 * 锁的连接与时序同当户模块：用一条【独立于事务的原始连接】在事务开启前 GET_LOCK、
 * 在事务【提交之后】才 RELEASE_LOCK，避免「锁已放、事务未提交」导致后到者算重号。
 */
@Repository
public class CollateralRepositoryImpl implements CollateralRepository {

    /** 业务日期统一按行里所在时区算，避免容器 UTC 下编号跨年。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
    /** 当物登记临界区命名锁（MySQL 全实例同名互斥）。 */
    private static final String WRITE_LOCK = "collateral:write";
    private static final int LOCK_WAIT_SECONDS = 10;
    private static final int MAX_RETRY = 5;

    private final CollateralMapper collateralMapper;
    private final TransactionTemplate transactionTemplate;
    private final DataSource dataSource;

    public CollateralRepositoryImpl(CollateralMapper collateralMapper,
                                    PlatformTransactionManager transactionManager,
                                    DataSource dataSource) {
        this.collateralMapper = collateralMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.dataSource = dataSource;
    }

    @Override
    public Mono<Collateral> insert(Collateral collateral) {
        return blocking(() -> {
            // 每轮重试用独立连接重新抢锁；兜住编号撞号 / 锁等待超时等瞬态冲突
            for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
                try {
                    return inWriteLock(() -> transactionTemplate.execute(status -> {
                        CollateralPO po = CollateralPoConverter.toPo(collateral);
                        po.setId(IdUtil.getSnowflakeNextId());
                        po.setItemNo(nextItemNo());
                        collateralMapper.insert(po);
                        return CollateralPoConverter.toDomain(po);
                    }));
                } catch (DuplicateKeyException | TransientDataAccessException e) {
                    // uk_item_no 是最后防线，锁内正常不会撞；撞了整段重新取号重试
                    if (attempt == MAX_RETRY - 1) {
                        throw new BizException("系统繁忙，请稍后重试");
                    }
                    try {
                        Thread.sleep(10L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BizException("系统繁忙，请稍后重试");
                    }
                }
            }
            throw new BizException("系统繁忙，请稍后重试");
        });
    }

    @Override
    public Mono<Collateral> update(Collateral collateral) {
        return blocking(() -> {
            CollateralPO existing = collateralMapper.selectById(collateral.getId());
            if (existing == null) {
                throw new BizException("当物不存在");
            }
            // 编号、所属当户永不改；把原值带回，避免被覆盖
            collateral.setItemNo(existing.getItemNo());
            collateral.setPawnerId(existing.getPawnerId());
            CollateralPO po = CollateralPoConverter.toPo(collateral);
            int rows = collateralMapper.updateById(po);
            if (rows == 0) {
                throw new BizException("当物不存在");
            }
            CollateralPO refreshed = collateralMapper.selectById(collateral.getId());
            return CollateralPoConverter.toDomain(Objects.requireNonNullElse(refreshed, po));
        });
    }

    @Override
    public Mono<Collateral> findById(Long id) {
        return blocking(() -> {
            CollateralPO po = collateralMapper.selectById(id);
            return po == null ? null : CollateralPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Collateral> findByItemNo(String itemNo) {
        return blocking(() -> {
            CollateralPO po = collateralMapper.selectOne(
                    Wrappers.<CollateralPO>lambdaQuery().eq(CollateralPO::getItemNo, itemNo));
            return po == null ? null : CollateralPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<Collateral>> page(int pageNum, int pageSize, CollateralQuery query) {
        return this.<PageResult<Collateral>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<CollateralPO> wrapper = Wrappers.<CollateralPO>lambdaQuery();
                if (query.pawnerId() != null) {
                    wrapper.eq(CollateralPO::getPawnerId, query.pawnerId());
                }
                if (query.category() != null) {
                    wrapper.eq(CollateralPO::getCategory, query.category().code());
                }
                if (query.conditionLevel() != null) {
                    wrapper.eq(CollateralPO::getConditionLevel, query.conditionLevel().code());
                }
                if (query.status() != null) {
                    wrapper.eq(CollateralPO::getStatus, query.status().code());
                }
                // 稳定排序：两页之间不会出现同一件，分页结果可重复对号
                wrapper.orderByAsc(CollateralPO::getId);
                List<CollateralPO> rows = collateralMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<Collateral> content = rows.stream()
                        .map(CollateralPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return blocking(() -> {
            // @TableLogic 自动改写为 UPDATE t_collateral SET del_flag=1：清单不再出现，物理行仍留库
            int rows = collateralMapper.deleteById(id);
            if (rows == 0) {
                throw new BizException("当物不存在");
            }
            return null;
        });
    }

    /**
     * 生成 DW-年份-序号：序号是当年已有编号（含已销账行）最大整数 +1，至少 4 位、超出自然进位。
     * 只在写锁（{@link #inWriteLock}）内调用，锁内串行所以不会撞号；
     * item_no 唯一索引是最后防线，极端瞬态冲突由外层整段重试兜底。
     */
    private String nextItemNo() {
        int year = LocalDate.now(BIZ_ZONE).getYear();
        String prefix = "DW-" + year + "-";
        long maxSeq = 0L;
        for (String no : collateralMapper.findItemNosByPrefix(prefix + "%")) {
            if (no == null || !no.startsWith(prefix)) {
                continue;
            }
            String tail = no.substring(prefix.length());
            if (tail.chars().allMatch(Character::isDigit)) {
                maxSeq = Math.max(maxSeq, Long.parseLong(tail));
            }
        }
        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 在全局命名锁保护下执行一段【含事务】的写入：锁由一条独立原始连接持有，
     * 在事务开始前 GET_LOCK、在事务提交/回滚之后才 RELEASE_LOCK（顺序不能颠倒）。
     *
     * 为什么锁要走独立连接而不是 MyBatis 连接：GET_LOCK 绑定连接；
     * 若用事务所在连接，Spring 提交时归还连接会立刻放锁，存在「锁已放、事务未提交」的窗口，
     * 后到的事务取号时读不到刚提交的最大序号，会算出重复号。独立连接持锁可把锁保到提交之后。
     */
    private <T> T inWriteLock(Supplier<T> action) {
        Connection lockConn;
        try {
            lockConn = dataSource.getConnection();
        } catch (SQLException e) {
            throw new BizException("系统繁忙，请稍后重试");
        }
        try {
            if (!namedLock(lockConn, true)) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            try {
                return action.get();
            } finally {
                // 此时 action 内的事务已提交（或回滚），放锁后后到者必能看到本次写入
                namedLock(lockConn, false);
            }
        } finally {
            try {
                lockConn.close();
            } catch (SQLException ignored) {
                // 连接关闭会自动释放其上的命名锁，不影响主流程
            }
        }
    }

    /** GET_LOCK / RELEASE_LOCK；返回 MySQL 结果（1 成功）。 */
    private boolean namedLock(Connection conn, boolean get) {
        String sql = get ? "SELECT GET_LOCK(?, ?)" : "SELECT RELEASE_LOCK(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WRITE_LOCK);
            if (get) {
                ps.setInt(2, LOCK_WAIT_SECONDS);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int r = rs.getInt(1);
                    return !rs.wasNull() && r == 1;
                }
                return false;
            }
        } catch (SQLException e) {
            if (get) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            return false;
        }
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic，
     * 操作人放进 AuditContextHolder 供审计填充（与当户模块同一套约定，顺序不能颠倒）。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
