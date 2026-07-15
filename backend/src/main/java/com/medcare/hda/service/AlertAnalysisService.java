package com.medcare.hda.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.MetricRules;
import com.medcare.hda.dto.AlertAnalysisItemVO;
import com.medcare.hda.dto.AlertAnalysisVO;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.entity.HealthMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 健康预警分析的查询、去重与持久化编排。 */
@Service
@RequiredArgsConstructor
public class AlertAnalysisService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("OPEN", "ACKNOWLEDGED", "IN_PROGRESS");

    private final HealthAlertService alertService;
    private final HealthMetricService metricService;
    private final AlertAnalysisEngine engine;

    public AlertAnalysisVO preview(Long userId) {
        LocalDateTime end = LocalDateTime.now();
        return analyze(userId, end.minusDays(7), end);
    }

    @Transactional
    public AlertAnalysisVO generate(Long userId) {
        LocalDateTime end = LocalDateTime.now();
        AlertAnalysisVO result = analyze(userId, end.minusDays(7), end);
        for (AlertAnalysisItemVO item : result.getAbnormalTypes()) {
            PersistResult persisted = persist(userId, item, null);
            if (persisted.alert() != null) result.getAlerts().add(persisted.alert());
            switch (persisted.outcome()) {
                case CREATED -> result.setGeneratedCount(result.getGeneratedCount() + 1);
                case UPDATED -> result.setUpdatedCount(result.getUpdatedCount() + 1);
                case DUPLICATE -> result.setDuplicateCount(result.getDuplicateCount() + 1);
            }
        }
        if (result.getGeneratedCount() > 0 || result.getUpdatedCount() > 0) {
            result.setConclusion("新增 " + result.getGeneratedCount() + " 条、更新 " + result.getUpdatedCount() + " 条预警"
                    + (result.getDuplicateCount() > 0 ? "，跳过 " + result.getDuplicateCount() + " 条重复结果" : ""));
        } else if (result.getDuplicateCount() > 0) {
            result.setConclusion("本批体征已经分析过，未重复生成预警");
        }
        return result;
    }

    /** 处理单条新保存或修改的体征，由事务提交后的领域事件自动调用。 */
    @Transactional
    public HealthAlert processMetric(HealthMetric metric) {
        if (!MetricRules.supports(metric.getMetricType()) || !MetricRules.judge(metric).abnormal()) return null;
        LocalDateTime end = LocalDateTime.now();
        AlertAnalysisVO analysis = analyze(metric.getUserId(), end.minusDays(7), end);
        AlertAnalysisItemVO item = analysis.getAbnormalTypes().stream()
                .filter(candidate -> metric.getMetricType().equals(candidate.getMetricType()))
                .findFirst().orElse(null);
        return item == null ? null : persist(metric.getUserId(), item, metric.getId()).alert();
    }

    private AlertAnalysisVO analyze(Long userId, LocalDateTime start, LocalDateTime end) {
        List<HealthMetric> metrics = metricService.list(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId)
                .ge(HealthMetric::getMeasureTime, start)
                .le(HealthMetric::getMeasureTime, end)
                .orderByDesc(HealthMetric::getMeasureTime));
        return engine.analyze(userId, metrics, start, end);
    }

    private PersistResult persist(Long userId, AlertAnalysisItemVO item, Long latestMetricId) {
        HealthAlert active = alertService.getOne(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId)
                .eq(HealthAlert::getAlertType, item.getTypeName() + "异常")
                .and(wrapper -> wrapper.in(HealthAlert::getStatus, ACTIVE_STATUSES)
                        .or().isNull(HealthAlert::getStatus))
                .orderByDesc(HealthAlert::getCreateTime)
                .last("LIMIT 1"), false);
        if (active != null) {
            if (item.getGenerationKey().equals(active.getGenerationKey())) {
                return new PersistResult(PersistOutcome.DUPLICATE, active);
            }
            active.setLevel(item.getLevel());
            active.setContent(buildContent(item));
            active.setGenerationKey(item.getGenerationKey());
            active.setReadFlag(0);
            active.setTriggerCount((active.getTriggerCount() == null ? 1 : active.getTriggerCount()) + 1);
            if (latestMetricId != null) active.setLatestMetricId(latestMetricId);
            active.setLastTriggerTime(LocalDateTime.now());
            if (!"IN_PROGRESS".equals(active.getStatus())) active.setStatus("OPEN");
            alertService.updateById(active);
            return new PersistResult(PersistOutcome.UPDATED, active);
        }

        boolean fingerprintExists = alertService.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId)
                .eq(HealthAlert::getGenerationKey, item.getGenerationKey())) > 0;
        if (fingerprintExists) return new PersistResult(PersistOutcome.DUPLICATE, null);

        HealthAlert alert = toAlert(userId, item, latestMetricId);
        try {
            alertService.save(alert);
            return new PersistResult(PersistOutcome.CREATED, alert);
        } catch (DuplicateKeyException ignored) {
            return new PersistResult(PersistOutcome.DUPLICATE, null);
        }
    }

    private HealthAlert toAlert(Long userId, AlertAnalysisItemVO item, Long latestMetricId) {
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setLevel(item.getLevel());
        alert.setAlertType(item.getTypeName() + "异常");
        alert.setContent(buildContent(item));
        alert.setReadFlag(0);
        alert.setStatus("OPEN");
        alert.setTriggerCount(1);
        alert.setLatestMetricId(latestMetricId);
        alert.setLastTriggerTime(LocalDateTime.now());
        alert.setGenerationKey(item.getGenerationKey());
        return alert;
    }

    private String buildContent(AlertAnalysisItemVO item) {
        String content = "近7日共 " + item.getAbnormalCount() + " 次" + item.getTypeName()
                + "异常。最近一次：" + item.getLatestMessage() + "。";
        if (!item.getLatestMessage().equals(item.getHighestRiskMessage())) {
            content += "七日内最高风险记录：" + item.getHighestRiskMessage() + "。";
        }
        return content;
    }

    private enum PersistOutcome { CREATED, UPDATED, DUPLICATE }
    private record PersistResult(PersistOutcome outcome, HealthAlert alert) {}
}
