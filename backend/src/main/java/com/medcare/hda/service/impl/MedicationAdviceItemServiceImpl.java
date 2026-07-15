package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.MedicationAdviceItem;
import com.medcare.hda.mapper.MedicationAdviceItemMapper;
import com.medcare.hda.service.MedicationAdviceItemService;
import org.springframework.stereotype.Service;

@Service
public class MedicationAdviceItemServiceImpl extends ServiceImpl<MedicationAdviceItemMapper, MedicationAdviceItem> implements MedicationAdviceItemService {
}
