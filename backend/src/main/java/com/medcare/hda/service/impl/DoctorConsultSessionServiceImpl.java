package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.mapper.DoctorConsultSessionMapper;
import com.medcare.hda.service.DoctorConsultSessionService;
import org.springframework.stereotype.Service;

@Service
public class DoctorConsultSessionServiceImpl
        extends ServiceImpl<DoctorConsultSessionMapper, DoctorConsultSession>
        implements DoctorConsultSessionService {
}
