package com.medcare.hda.event;

/** 体征事务成功后触发的领域事件。 */
public record HealthMetricChangedEvent(Long userId, Long metricId) {
}
