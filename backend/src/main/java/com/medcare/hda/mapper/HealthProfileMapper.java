package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.HealthProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfile> {
}
