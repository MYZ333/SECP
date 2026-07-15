package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.MedicationAdvice;
import com.medcare.hda.mapper.MedicationAdviceMapper;
import com.medcare.hda.service.MedicationAdviceService;
import org.springframework.stereotype.Service;

@Service
public class MedicationAdviceServiceImpl extends ServiceImpl<MedicationAdviceMapper, MedicationAdvice> implements MedicationAdviceService {
}
