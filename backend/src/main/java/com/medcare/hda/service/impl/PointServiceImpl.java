package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.entity.PointProduct;
import com.medcare.hda.entity.PointRecord;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.mapper.PointExchangeMapper;
import com.medcare.hda.mapper.PointProductMapper;
import com.medcare.hda.mapper.PointRecordMapper;
import com.medcare.hda.mapper.UserMapper;
import com.medcare.hda.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserMapper userMapper;
    private final PointRecordMapper pointRecordMapper;
    private final PointProductMapper pointProductMapper;
    private final PointExchangeMapper pointExchangeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointRecord addPoints(Long userId, Integer change, String type, String description) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int current = user.getPoints() == null ? 0 : user.getPoints();
        int balance = current + change;
        if (balance < 0) {
            throw new BusinessException(ResultCode.POINTS_NOT_ENOUGH);
        }
        user.setPoints(balance);
        userMapper.updateById(user);

        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setChangePoints(change);
        record.setBalance(balance);
        record.setType(type);
        record.setDescription(description);
        pointRecordMapper.insert(record);
        return record;
    }

    @Override
    public IPage<PointRecord> pageRecords(Long userId, long pageNum, long pageSize) {
        return pointRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PointRecord>lambdaQuery()
                        .eq(PointRecord::getUserId, userId)
                        .orderByDesc(PointRecord::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointExchange exchange(Long userId, ExchangeDTO dto) {
        int quantity = dto.getQuantity() == null || dto.getQuantity() < 1 ? 1 : dto.getQuantity();
        PointProduct product = pointProductMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (product.getStock() != null && product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        int cost = product.getPointsCost() * quantity;

        // 扣积分并写流水
        addPoints(userId, -cost, "EXCHANGE", "兑换商品: " + product.getName());

        // 扣库存
        if (product.getStock() != null) {
            product.setStock(product.getStock() - quantity);
            pointProductMapper.updateById(product);
        }

        PointExchange exchange = new PointExchange();
        exchange.setUserId(userId);
        exchange.setProductId(product.getId());
        exchange.setProductName(product.getName());
        exchange.setPointsCost(cost);
        exchange.setQuantity(quantity);
        exchange.setStatus(0);
        exchange.setAddress(dto.getAddress());
        pointExchangeMapper.insert(exchange);
        return exchange;
    }

    @Override
    public IPage<PointExchange> pageExchanges(Long userId, long pageNum, long pageSize) {
        return pointExchangeMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PointExchange>lambdaQuery()
                        .eq(PointExchange::getUserId, userId)
                        .orderByDesc(PointExchange::getCreateTime));
    }
}
