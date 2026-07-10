package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.MedicalRecord;
import com.medcare.hda.mapper.MedicalRecordMapper;
import com.medcare.hda.service.MedicalRecordService;
import org.springframework.stereotype.Service;

@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecord> implements MedicalRecordService {
}
