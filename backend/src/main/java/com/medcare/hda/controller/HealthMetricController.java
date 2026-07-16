package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.MetricRules;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.event.HealthMetricChangedEvent;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.HealthMetricService;
import com.medcare.hda.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康档案-体征/体检数据")
@RestController
@RequestMapping("/api/health/metric")
@RequiredArgsConstructor
public class HealthMetricController {

    private final HealthMetricService service;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "分页查询(当前用户)")
    @GetMapping("/page")
    public Result<PageResult<HealthMetric>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String metricType) {
        Long userId = SecurityUtil.getUserId();
        IPage<HealthMetric> page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<HealthMetric>lambdaQuery()
                        .eq(HealthMetric::getUserId, userId)
                        .eq(StringUtils.hasText(metricType), HealthMetric::getMetricType, metricType)
                        .orderByDesc(HealthMetric::getMeasureTime));
        // 展示时按最新阈值规则实时复判（不落库），保证规则上线前的历史数据也能正确标注异常
        page.getRecords().forEach(MetricRules::apply);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<HealthMetric> get(@PathVariable Long id) {
        HealthMetric entity = service.getById(id);
        checkOwner(entity);
        return Result.success(entity);
    }

    @Operation(summary = "新增(自动按阈值判断是否异常; 测量时间不填默认当前时间)")
    @PostMapping
    @Transactional
    public Result<HealthMetric> create(@RequestBody HealthMetric entity) {
        entity.setId(null);
        entity.setUserId(SecurityUtil.getUserId());
        if (entity.getMeasureTime() == null) {
            entity.setMeasureTime(java.time.LocalDateTime.now());
        }
        validateMetric(entity);
        MetricRules.Judge judge = MetricRules.apply(entity);
        service.save(entity);
        // 积分任务: 记录健康数据后标记为"待领取"（到积分中心手动领取）
        pointService.markTaskReady(entity.getUserId(), "METRIC");
        eventPublisher.publishEvent(new HealthMetricChangedEvent(entity.getUserId(), entity.getId()));
        String msg = judge.abnormal()
                ? "已记录，系统将自动创建或更新健康预警。注意：" + judge.message()
                : "新增成功";
        return Result.success(msg, entity);
    }

    @Operation(summary = "修改(自动按阈值判断是否异常)")
    @PutMapping
    @Transactional
    public Result<HealthMetric> update(@RequestBody HealthMetric entity) {
        HealthMetric exist = service.getById(entity.getId());
        checkOwner(exist);
        entity.setUserId(SecurityUtil.getUserId());
        validateMetric(entity);
        MetricRules.Judge judge = MetricRules.apply(entity);
        service.updateById(entity);
        eventPublisher.publishEvent(new HealthMetricChangedEvent(entity.getUserId(), entity.getId()));
        String msg = judge.abnormal()
                ? "修改成功，系统将自动创建或更新健康预警。注意：" + judge.message()
                : "修改成功";
        return Result.success(msg, entity);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        HealthMetric exist = service.getById(id);
        checkOwner(exist);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    private void checkOwner(HealthMetric entity) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!entity.getUserId().equals(SecurityUtil.getUserId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void validateMetric(HealthMetric entity) {
        String message = MetricRules.validationMessage(entity);
        if (message != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }
}
