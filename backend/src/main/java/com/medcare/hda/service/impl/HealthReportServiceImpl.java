package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.mapper.HealthReportMapper;
import com.medcare.hda.service.HealthReportService;
import org.springframework.stereotype.Service;

@Service
public class HealthReportServiceImpl extends ServiceImpl<HealthReportMapper, HealthReport> implements HealthReportService {
}
