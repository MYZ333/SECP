package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT COUNT(*)
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE r.code = 'PATIENT'
              AND r.status = 1
              AND u.status = 0
              AND u.deleted = 0
            """)
    long countActivePatients();

    @Select("""
            <script>
            SELECT u.*
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE r.code = 'PATIENT'
              AND r.status = 1
              AND u.status = 0
              AND u.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY u.create_time DESC
            </script>
            """)
    IPage<User> pageActivePatients(Page<User> page, @Param("keyword") String keyword);
}
