package com.medcare.hda.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medcare.hda.dto.CheckInVO;
import com.medcare.hda.dto.ClaimVO;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.dto.PointTaskVO;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.entity.PointRecord;

import java.util.List;

public interface PointService {

    /** 增加/扣减积分并写流水；change 正数为获得，负数为消耗 */
    PointRecord addPoints(Long userId, Integer change, String type, String description);

    /** 每日签到：基础分 + 连续签到加成；今日已签到抛出业务异常 */
    CheckInVO checkIn(Long userId);

    /** 每日任务奖励：同类型每天最多发一次；失败不影响主流程 */
    void rewardDaily(Long userId, String type, int points, String description);

    /** 一次性任务奖励：同类型只发一次；失败不影响主流程 */
    void rewardOnce(Long userId, String type, int points, String description);

    /** 标记某任务"已完成待领取"（条件达成时调用，不直接发分） */
    void markTaskReady(Long userId, String type);

    /** 领取某任务积分：校验已完成且未领取，发放积分并返回结果 */
    ClaimVO claimTask(Long userId, String type);

    /** 积分任务列表(各获取方式及完成状态) */
    List<PointTaskVO> listTasks(Long userId);

    /** 分页查询用户积分明细 */
    IPage<PointRecord> pageRecords(Long userId, long pageNum, long pageSize);

    /** 积分兑换商品 */
    PointExchange exchange(Long userId, ExchangeDTO dto);

    /** 分页查询用户兑换记录 */
    IPage<PointExchange> pageExchanges(Long userId, long pageNum, long pageSize);
}
