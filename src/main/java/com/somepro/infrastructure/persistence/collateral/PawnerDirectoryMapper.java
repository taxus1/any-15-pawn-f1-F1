package com.somepro.infrastructure.persistence.collateral;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当户表只读 Mapper：当物模块只在「登记认人」时查当户状态，不做任何写入。
 * 状态口径见 doc/schema/pawn.sql：NORMAL 正常 / FROZEN 冻结 / CLOSED 注销。
 */
@Mapper
public interface PawnerDirectoryMapper {

    /** 当户当前状态码；查无此人（或档案已逻辑删除）返回 null。 */
    @Select("SELECT status FROM t_pawner WHERE id = #{pawnerId} AND del_flag = 0")
    String findStatus(@Param("pawnerId") Long pawnerId);
}
