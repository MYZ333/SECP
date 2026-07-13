package com.medcare.hda.agent.core;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SafetyTriageService {
    private static final List<String> EMERGENCY = List.of(
            "呼吸困难", "喘不上气", "意识不清", "失去意识", "昏迷", "抽搐",
            "口角歪斜", "言语不清", "一侧无力", "大量出血", "严重过敏", "喉咙肿",
            "想自杀", "不想活", "伤害自己", "伤害别人",
            "我要猝死了", "快猝死了", "要猝死了", "感觉要猝死", "快死了", "要死了", "濒死感"
    );

    public RiskAssessment assess(String message, HealthContext context) {
        String normalized = message == null ? "" : message.replaceAll("\\s+", "");
        boolean chestEmergency = positive(normalized, "胸痛") &&
                (positive(normalized, "出汗") || positive(normalized, "冒冷汗") || positive(normalized, "呼吸困难"));
        if (chestEmergency) return emergency("胸痛并伴随出汗或呼吸困难属于需要立即处理的危险信号");
        for (String keyword : EMERGENCY) {
            if (positive(normalized, keyword)) return emergency("描述中出现“" + keyword + "”等危险信号");
        }
        if (context.highRiskMetric()) {
            return new RiskAssessment("HIGH", "近期体征记录存在高风险异常，建议尽快由医疗专业人员评估。", false);
        }
        if (containsAny(normalized, List.of("越来越严重", "持续加重", "高烧不退", "剧烈疼痛", "孕妇", "婴儿"))) {
            return new RiskAssessment("MEDIUM", "当前情况需要更谨慎地评估，若持续或加重请及时就医。", false);
        }
        return new RiskAssessment("LOW", "暂未从描述中识别到明确的紧急危险信号。", false);
    }

    private RiskAssessment emergency(String reason) {
        return new RiskAssessment("EMERGENCY", reason + "。请立即呼叫当地急救电话（中国大陆可拨打120）或前往急诊，不要等待线上回复。", true);
    }

    private boolean positive(String text, String keyword) {
        int index = text.indexOf(keyword);
        if (index < 0) return false;
        String prefix = text.substring(Math.max(0, index - 5), index);
        return !(prefix.contains("没有") || prefix.contains("无") || prefix.contains("否认") || prefix.contains("并未") || prefix.contains("不是"));
    }

    private boolean containsAny(String text, List<String> values) { return values.stream().anyMatch(text::contains); }
}
