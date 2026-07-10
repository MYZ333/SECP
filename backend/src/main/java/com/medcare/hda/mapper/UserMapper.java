package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
