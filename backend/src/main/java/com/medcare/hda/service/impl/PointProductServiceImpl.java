package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.PointProduct;
import com.medcare.hda.mapper.PointProductMapper;
import com.medcare.hda.service.PointProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointProductServiceImpl extends ServiceImpl<PointProductMapper, PointProduct> implements PointProductService {

    @Override
    public List<String> listActiveCategories() {
        return listObjs(
                Wrappers.<PointProduct>query()
                        .select("DISTINCT category")
                        .eq("status", 1)
                        .isNotNull("category"),
                Object::toString);
    }
}
