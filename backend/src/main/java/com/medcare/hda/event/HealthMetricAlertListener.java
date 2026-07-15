package com.medcare.hda.event;

import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.service.AlertAnalysisService;
import com.medcare.hda.service.HealthMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 体征保存事务提交后自动创建或更新预警。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthMetricAlertListener {
    private final HealthMetricService metricService;
    private final AlertAnalysisService alertAnalysisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMetricChanged(HealthMetricChangedEvent event) {
        try {
            HealthMetric metric = metricService.getById(event.metricId());
            if (metric != null && event.userId().equals(metric.getUserId())) {
                alertAnalysisService.processMetric(metric);
            }
        } catch (Exception e) {
            // 体征已经成功保存，预警处理失败不能反向影响用户录入；记录日志供补偿任务处理。
            log.error("自动生成健康预警失败, userId={}, metricId={}", event.userId(), event.metricId(), e);
        }
    }
}
