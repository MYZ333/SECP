package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.annotation.DistributedLock;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.CheckInVO;
import com.medcare.hda.dto.ClaimVO;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.dto.PointTaskVO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    /** 签到基础分 */
    private static final int CHECKIN_BASE = 2;
    /** 连签加成上限(连续7天及以上每天可得 2+6=8 分) */
    private static final int CHECKIN_MAX_BONUS = 6;

    private final UserMapper userMapper;
    private final PointRecordMapper pointRecordMapper;
    private final PointProductMapper pointProductMapper;
    private final PointExchangeMapper pointExchangeMapper;
    private final StringRedisTemplate redis;

    private static final String READY_PREFIX = "hda:task:ready:";
    private static final String CLAIMED_PREFIX = "hda:task:claimed:";

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
    @Transactional(rollbackFor = Exception.class)
    public CheckInVO checkIn(Long userId) {
        if (hasRecordOn(userId, "CHECKIN", LocalDate.now())) {
            throw new BusinessException(ResultCode.ALREADY_CHECKED_IN);
        }
        int streak = calcStreak(userId);
        int points = CHECKIN_BASE + Math.min(streak - 1, CHECKIN_MAX_BONUS);
        PointRecord record = addPoints(userId, points, "CHECKIN", "每日签到(连续" + streak + "天)");
        return CheckInVO.builder()
                .points(points)
                .streakDays(streak)
                .balance(record.getBalance())
                .build();
    }

    @Override
    public void rewardDaily(Long userId, String type, int points, String description) {
        try {
            if (hasRecordOn(userId, type, LocalDate.now())) {
                return;
            }
            addPoints(userId, points, type, description);
        } catch (Exception e) {
            log.warn("发放每日积分失败 userId={} type={}: {}", userId, type, e.getMessage());
        }
    }

    @Override
    public void rewardOnce(Long userId, String type, int points, String description) {
        try {
            Long count = pointRecordMapper.selectCount(Wrappers.<PointRecord>lambdaQuery()
                    .eq(PointRecord::getUserId, userId)
                    .eq(PointRecord::getType, type));
            if (count != null && count > 0) {
                return;
            }
            addPoints(userId, points, type, description);
        } catch (Exception e) {
            log.warn("发放一次性积分失败 userId={} type={}: {}", userId, type, e.getMessage());
        }
    }

    @Override
    public List<PointTaskVO> listTasks(Long userId) {
        LocalDate today = LocalDate.now();
        boolean checkedIn = hasRecordOn(userId, "CHECKIN", today);
        int streak = calcStreak(userId);
        int checkinPoints = CHECKIN_BASE + Math.min(streak - 1, CHECKIN_MAX_BONUS);

        List<PointTaskVO> tasks = new ArrayList<>();
        // 签到走顶部横幅的即时按钮，状态只有 已完成/去完成
        tasks.add(PointTaskVO.builder()
                .type("CHECKIN").name("每日签到")
                .description("基础2分, 连续签到每天+1, 最高8分")
                .points(checkinPoints).daily(true).done(checkedIn)
                .status(checkedIn ? "DONE" : "TODO").streakDays(streak)
                .build());
        // 每日任务：完成后进入"待领取"，需手动领取
        tasks.add(buildDailyTask(userId, "LOGIN", "每日登录", "登录后即可领取每日积分", 1));
        tasks.add(buildDailyTask(userId, "METRIC", "记录健康数据", "记录血压/血糖等体征后可领取", 2));
        tasks.add(buildDailyTask(userId, "CONSULT", "AI健康咨询", "使用AI健康咨询后可领取", 2));
        // 一次性任务：完善健康档案
        boolean profileClaimed = isTaskClaimed(userId, "PROFILE");
        boolean profileReady = Boolean.TRUE.equals(redis.hasKey(readyKey(userId, "PROFILE")));
        tasks.add(PointTaskVO.builder()
                .type("PROFILE").name("完善健康档案")
                .description("填写身高、体重、血型等基本信息后可领取(一次性)")
                .points(20).daily(false).done(profileClaimed)
                .status(profileClaimed ? "DONE" : (profileReady ? "CLAIMABLE" : "TODO"))
                .build());
        return tasks;
    }

    /** 构造每日任务 VO：按 待领取/已领取/去完成 计算状态 */
    private PointTaskVO buildDailyTask(Long userId, String type, String name, String desc, int points) {
        boolean claimed = isTaskClaimed(userId, type);
        boolean ready = Boolean.TRUE.equals(redis.hasKey(readyKey(userId, type)));
        String status = claimed ? "DONE" : (ready ? "CLAIMABLE" : "TODO");
        return PointTaskVO.builder()
                .type(type).name(name).description(desc)
                .points(points).daily(true).done(claimed).status(status)
                .build();
    }

    /** 是否为需领取的任务类型 */
    private boolean isClaimableTask(String type) {
        return switch (type) {
            case "LOGIN", "METRIC", "CONSULT", "PROFILE" -> true;
            default -> false;
        };
    }

    private boolean isDailyTask(String type) {
        return !"PROFILE".equals(type);
    }

    private int taskPoints(String type) {
        return switch (type) {
            case "LOGIN" -> 1;
            case "METRIC", "CONSULT" -> 2;
            case "PROFILE" -> 20;
            default -> 0;
        };
    }

    private String taskDesc(String type) {
        return switch (type) {
            case "LOGIN" -> "每日登录奖励";
            case "METRIC" -> "记录健康数据奖励";
            case "CONSULT" -> "AI健康咨询奖励";
            case "PROFILE" -> "完善健康档案奖励";
            default -> "任务奖励";
        };
    }

    private String readyKey(Long userId, String type) {
        return isDailyTask(type)
                ? READY_PREFIX + type + ":" + userId + ":" + LocalDate.now()
                : READY_PREFIX + type + ":" + userId;
    }

    private String claimedKey(Long userId, String type) {
        return isDailyTask(type)
                ? CLAIMED_PREFIX + type + ":" + userId + ":" + LocalDate.now()
                : CLAIMED_PREFIX + type + ":" + userId;
    }

    /** 已领取判定：领取标记存在，或已有对应积分流水(兼容历史自动发放的数据) */
    private boolean isTaskClaimed(Long userId, String type) {
        if (Boolean.TRUE.equals(redis.hasKey(claimedKey(userId, type)))) {
            return true;
        }
        if (isDailyTask(type)) {
            return hasRecordOn(userId, type, LocalDate.now());
        }
        Long count = pointRecordMapper.selectCount(Wrappers.<PointRecord>lambdaQuery()
                .eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, type));
        return count != null && count > 0;
    }

    @Override
    public void markTaskReady(Long userId, String type) {
        try {
            if (!isClaimableTask(type)) {
                return;
            }
            if (isDailyTask(type)) {
                redis.opsForValue().set(readyKey(userId, type), "1", 26, TimeUnit.HOURS);
            } else {
                redis.opsForValue().set(readyKey(userId, type), "1");
            }
        } catch (Exception e) {
            log.warn("标记任务待领取失败 userId={} type={}: {}", userId, type, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClaimVO claimTask(Long userId, String type) {
        if (!isClaimableTask(type)) {
            throw new BusinessException("未知的任务类型");
        }
        // 条件未完成不可领取
        if (!Boolean.TRUE.equals(redis.hasKey(readyKey(userId, type)))) {
            throw new BusinessException("任务尚未完成，无法领取");
        }
        // 原子占位，防并发/重复领取
        String ck = claimedKey(userId, type);
        Boolean first = isDailyTask(type)
                ? redis.opsForValue().setIfAbsent(ck, "1", 26, TimeUnit.HOURS)
                : redis.opsForValue().setIfAbsent(ck, "1");
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(isDailyTask(type) ? "该奖励今日已领取" : "该奖励已领取");
        }
        try {
            PointRecord record = addPoints(userId, taskPoints(type), type, taskDesc(type));
            return ClaimVO.builder().points(taskPoints(type)).balance(record.getBalance()).build();
        } catch (RuntimeException e) {
            redis.delete(ck); // 发分失败回滚领取占位，允许重试
            throw e;
        }
    }

    /** 某天是否已有某类型的积分流水 */
    private boolean hasRecordOn(Long userId, String type, LocalDate day) {
        Long count = pointRecordMapper.selectCount(Wrappers.<PointRecord>lambdaQuery()
                .eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, type)
                .ge(PointRecord::getCreateTime, day.atStartOfDay())
                .lt(PointRecord::getCreateTime, day.plusDays(1).atStartOfDay()));
        return count != null && count > 0;
    }

    /** 连续签到天数(含今天): 1 + 从昨天起向前连续签到的天数 */
    private int calcStreak(Long userId) {
        return 1 + countConsecutiveFrom(userId, LocalDate.now().minusDays(1));
    }

    /** 从指定日期向前数连续有 CHECKIN 流水的天数 */
    private int countConsecutiveFrom(Long userId, LocalDate from) {
        List<PointRecord> recent = pointRecordMapper.selectList(Wrappers.<PointRecord>lambdaQuery()
                .eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, "CHECKIN")
                .orderByDesc(PointRecord::getCreateTime)
                .last("LIMIT 60"));
        int days = 0;
        LocalDate expect = from;
        for (PointRecord r : recent) {
            if (r.getCreateTime() == null) {
                continue;
            }
            LocalDate d = r.getCreateTime().toLocalDate();
            if (d.isAfter(expect)) {
                continue; // 今天的记录, 跳过
            }
            if (d.equals(expect)) {
                days++;
                expect = expect.minusDays(1);
            } else {
                break;
            }
        }
        return days;
    }

    @Override
    public IPage<PointRecord> pageRecords(Long userId, long pageNum, long pageSize) {
        return pointRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PointRecord>lambdaQuery()
                        .eq(PointRecord::getUserId, userId)
                        .orderByDesc(PointRecord::getCreateTime));
    }

    @Override
    @DistributedLock(key = "'exchange:' + #userId", waitSeconds = 3, leaseSeconds = 10,
            message = "兑换处理中，请勿重复操作")
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
