package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.mapper.PointExchangeMapper;
import com.medcare.hda.service.PointExchangeService;
import org.springframework.stereotype.Service;

@Service
public class PointExchangeServiceImpl extends ServiceImpl<PointExchangeMapper, PointExchange> implements PointExchangeService {
}
