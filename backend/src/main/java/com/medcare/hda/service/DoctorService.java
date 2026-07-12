package com.medcare.hda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medcare.hda.entity.Doctor;

import java.util.List;

public interface DoctorService extends IService<Doctor> {

    /** 医生详情（Redis 缓存，读多写少） */
    Doctor getCachedById(Long id);

    /** 在库科室列表（Redis 缓存） */
    List<String> listActiveDepartments();
}
