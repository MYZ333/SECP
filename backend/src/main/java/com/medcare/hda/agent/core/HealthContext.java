package com.medcare.hda.agent.core;

import java.util.List;

public record HealthContext(String summary, List<String> categories, boolean highRiskMetric) {
    public static HealthContext empty() { return new HealthContext("未授权读取健康档案", List.of(), false); }
}
