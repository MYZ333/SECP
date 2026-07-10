package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.HealthReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthReportMapper extends BaseMapper<HealthReport> {
}
