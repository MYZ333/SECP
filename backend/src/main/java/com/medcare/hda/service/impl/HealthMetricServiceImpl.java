package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.mapper.HealthMetricMapper;
import com.medcare.hda.service.HealthMetricService;
import org.springframework.stereotype.Service;

@Service
public class HealthMetricServiceImpl extends ServiceImpl<HealthMetricMapper, HealthMetric> implements HealthMetricService {
}
