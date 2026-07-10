package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.ConsultRecord;
import com.medcare.hda.mapper.ConsultRecordMapper;
import com.medcare.hda.service.ConsultRecordService;
import org.springframework.stereotype.Service;

@Service
public class ConsultRecordServiceImpl extends ServiceImpl<ConsultRecordMapper, ConsultRecord> implements ConsultRecordService {
}
