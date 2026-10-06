package com.somepro.domain.collateral.repository;

import reactor.core.publisher.Mono;

/**
 * 当户名册端口：当物模块登记时要「认人」——只问当户模块一件事：这个人能不能挂东西。
 * （领域层定义，基础设施层跨表实现，只读 t_pawner，不写。）
 *
 * 当物模块自己的表不存当户信息冗余 —— 存了就会和当户档案对不上。
 */
public interface PawnerDirectoryPort {

    /**
     * 该当户是否「在册」（存在且未注销）：true 可挂当物；false 已注销（CLOSED）；
     * 空 Mono 表示查无此人（或档案已逻辑删除）。
     */
    Mono<Boolean> isActive(Long pawnerId);
}
