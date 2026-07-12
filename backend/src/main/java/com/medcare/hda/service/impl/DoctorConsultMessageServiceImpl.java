package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.DoctorConsultMessage;
import com.medcare.hda.mapper.DoctorConsultMessageMapper;
import com.medcare.hda.service.DoctorConsultMessageService;
import org.springframework.stereotype.Service;

@Service
public class DoctorConsultMessageServiceImpl
        extends ServiceImpl<DoctorConsultMessageMapper, DoctorConsultMessage>
        implements DoctorConsultMessageService {
}
