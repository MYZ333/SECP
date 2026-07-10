package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.mapper.HealthProfileMapper;
import com.medcare.hda.service.HealthProfileService;
import org.springframework.stereotype.Service;

@Service
public class HealthProfileServiceImpl extends ServiceImpl<HealthProfileMapper, HealthProfile> implements HealthProfileService {
}
