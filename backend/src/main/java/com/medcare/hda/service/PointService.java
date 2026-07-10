package com.medcare.hda.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.entity.PointRecord;

public interface PointService {

    /** 增加/扣减积分并写流水；change 正数为获得，负数为消耗 */
    PointRecord addPoints(Long userId, Integer change, String type, String description);

    /** 分页查询用户积分明细 */
    IPage<PointRecord> pageRecords(Long userId, long pageNum, long pageSize);

    /** 积分兑换商品 */
    PointExchange exchange(Long userId, ExchangeDTO dto);

    /** 分页查询用户兑换记录 */
    IPage<PointExchange> pageExchanges(Long userId, long pageNum, long pageSize);
}
