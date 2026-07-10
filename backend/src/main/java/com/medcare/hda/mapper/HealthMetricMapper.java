package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.HealthMetric;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthMetricMapper extends BaseMapper<HealthMetric> {
}
