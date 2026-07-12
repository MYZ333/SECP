package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.mapper.DoctorMapper;
import com.medcare.hda.service.DoctorService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Override
    @Cacheable(value = "doctor:detail", key = "#id", unless = "#result == null")
    public Doctor getCachedById(Long id) {
        return getById(id);
    }

    @Override
    public List<String> listActiveDepartments() {
        // 科室列表随医生数据实时变化（且常通过 SQL 直接维护），不缓存，避免筛选 tab 显示过期
        return listObjs(
                Wrappers.<Doctor>query()
                        .select("DISTINCT department")
                        .eq("status", 1)
                        .isNotNull("department")
                        .orderByAsc("department"),
                Objects::toString);
    }

    /** 医生被修改/删除时，清空详情缓存，保证后台改动立即生效 */
    @Override
    @CacheEvict(value = "doctor:detail", allEntries = true)
    public boolean updateById(Doctor entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "doctor:detail", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
