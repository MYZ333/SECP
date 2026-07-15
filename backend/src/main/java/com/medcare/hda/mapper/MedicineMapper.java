package com.medcare.hda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medcare.hda.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {
}
