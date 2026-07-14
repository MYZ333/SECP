package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
            ORDER BY FIELD(r.code, 'ADMIN', 'PATIENT', 'DOCTOR'), r.id
            """)
    List<String> selectRoleCodesByUserId(Long userId);
}
