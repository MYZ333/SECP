package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.PointProduct;
import com.medcare.hda.mapper.PointProductMapper;
import com.medcare.hda.service.PointProductService;
import org.springframework.stereotype.Service;

@Service
public class PointProductServiceImpl extends ServiceImpl<PointProductMapper, PointProduct> implements PointProductService {
}
