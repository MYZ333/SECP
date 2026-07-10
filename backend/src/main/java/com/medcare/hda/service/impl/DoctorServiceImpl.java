package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.mapper.DoctorMapper;
import com.medcare.hda.service.DoctorService;
import org.springframework.stereotype.Service;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {
}
