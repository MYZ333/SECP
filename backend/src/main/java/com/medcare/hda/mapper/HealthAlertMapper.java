package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.HealthAlert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthAlertMapper extends BaseMapper<HealthAlert> {
}
