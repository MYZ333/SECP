package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.mapper.HealthAlertMapper;
import com.medcare.hda.service.HealthAlertService;
import org.springframework.stereotype.Service;

@Service
public class HealthAlertServiceImpl extends ServiceImpl<HealthAlertMapper, HealthAlert> implements HealthAlertService {
}
