package com.medcare.hda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medcare.hda.entity.PointProduct;

import java.util.List;

public interface PointProductService extends IService<PointProduct> {

    /** 上架商品类别列表（Redis 缓存，读多写少） */
    List<String> listActiveCategories();
}
