package com.medcare.hda.common;

import com.medcare.hda.entity.HealthMetric;

/**
 * 体征阈值规则：录入/修改体征时自动判断是否异常，并为预警生成提供级别与描述。
 * 参考《中国高血压防治指南》《中国2型糖尿病防治指南》常用成人阈值，仅作健康提示，不构成诊断。
 */
public final class MetricRules {

    private MetricRules() {}

    /** 判断结果 */
    public record Judge(boolean abnormal, String level, String message) {}

    /** 自动判断并回填 abnormal 字段，返回判断详情 */
    public static Judge apply(HealthMetric m) {
        Judge j = judge(m);
        m.setAbnormal(j.abnormal() ? 1 : 0);
        return j;
    }

    public static Judge judge(HealthMetric m) {
        Double v1 = m.getMetricValue();
        Double v2 = m.getMetricValue2();
        String type = m.getMetricType() == null ? "" : m.getMetricType();
        if (v1 == null) {
            return normal();
        }
        return switch (type) {
            case "BLOOD_PRESSURE" -> bloodPressure(v1, v2);
            case "BLOOD_SUGAR" -> bloodSugar(v1);
            case "HEART_RATE" -> heartRate(v1);
            case "TEMPERATURE" -> temperature(v1);
            default -> normal(); // WEIGHT 等暂不判断
        };
    }

    /** 是否为当前规则引擎支持分析的指标类型 */
    public static boolean supports(String type) {
        return "BLOOD_PRESSURE".equals(type)
                || "BLOOD_SUGAR".equals(type)
                || "HEART_RATE".equals(type)
                || "TEMPERATURE".equals(type);
    }

    /**
     * 校验体征是否落在常见设备可录入范围。这里负责识别明显误录，
     * 与下面用于健康提示的异常阈值是两层不同规则。
     */
    public static String validationMessage(HealthMetric m) {
        String type = m.getMetricType() == null ? "" : m.getMetricType();
        Double v1 = m.getMetricValue();
        Double v2 = m.getMetricValue2();
        if (v1 == null) return "请输入体征数值";
        return switch (type) {
            case "BLOOD_PRESSURE" -> {
                if (v2 == null) yield "请输入舒张压";
                if (v1 < 50 || v1 > 260 || v2 < 30 || v2 > 180 || v1 <= v2) {
                    yield "血压数值明显超出常见测量范围，请检查收缩压、舒张压或重新测量";
                }
                yield null;
            }
            case "BLOOD_SUGAR" -> v1 < 1 || v1 > 40
                    ? "血糖数值明显超出常见测量范围（1–40 mmol/L），请检查单位或重新测量" : null;
            case "HEART_RATE" -> v1 < 25 || v1 > 250
                    ? "心率数值明显超出常见测量范围（25–250 次/分），请检查或重新测量" : null;
            case "TEMPERATURE" -> v1 < 30 || v1 > 45
                    ? "体温数值明显超出常见测量范围（30–45 ℃），请检查或重新测量" : null;
            case "WEIGHT" -> v1 < 2 || v1 > 350
                    ? "体重数值明显超出常见录入范围（2–350 kg），请检查单位" : null;
            default -> null;
        };
    }

    /** 指标类型的中文名 */
    public static String typeName(String type) {
        return switch (type == null ? "" : type) {
            case "BLOOD_PRESSURE" -> "血压";
            case "BLOOD_SUGAR" -> "血糖";
            case "HEART_RATE" -> "心率";
            case "TEMPERATURE" -> "体温";
            case "WEIGHT" -> "体重";
            default -> type;
        };
    }

    private static Judge bloodPressure(double sys, Double diaObj) {
        double dia = diaObj == null ? 0 : diaObj;
        if (sys >= 160 || dia >= 100) {
            return high(String.format("血压 %.0f/%.0f mmHg，达到2级高血压水平，建议尽快就医", sys, dia));
        }
        if (sys >= 140 || dia >= 90) {
            return medium(String.format("血压 %.0f/%.0f mmHg，高于正常值(140/90)，建议复测并注意低盐饮食", sys, dia));
        }
        if (sys < 90 || (diaObj != null && dia < 60)) {
            return medium(String.format("血压 %.0f/%.0f mmHg，偏低，注意起身缓慢、防跌倒", sys, dia));
        }
        return normal();
    }

    private static Judge bloodSugar(double v) {
        if (v >= 11.1) {
            return high(String.format("血糖 %.1f mmol/L，明显偏高(≥11.1)，建议尽快就医复查", v));
        }
        if (v >= 7.0) {
            return medium(String.format("血糖 %.1f mmol/L，高于空腹正常值(7.0)，建议控制饮食并复测", v));
        }
        if (v < 3.9) {
            return high(String.format("血糖 %.1f mmol/L，低血糖(＜3.9)有风险，请立即进食并观察", v));
        }
        return normal();
    }

    private static Judge heartRate(double v) {
        if (v > 120 || v < 50) {
            return high(String.format("心率 %.0f 次/分，明显异常，建议尽快就医检查", v));
        }
        if (v > 100 || v < 60) {
            return medium(String.format("心率 %.0f 次/分，超出正常范围(60-100)，建议休息后复测", v));
        }
        return normal();
    }

    private static Judge temperature(double v) {
        if (v >= 39.0) {
            return high(String.format("体温 %.1f ℃，高热，建议及时就医", v));
        }
        if (v >= 37.3) {
            return medium(String.format("体温 %.1f ℃，发热(≥37.3)，注意休息多饮水并复测", v));
        }
        if (v < 36.0) {
            return medium(String.format("体温 %.1f ℃，偏低，注意保暖并复测", v));
        }
        return normal();
    }

    private static Judge normal() { return new Judge(false, null, null); }
    private static Judge medium(String msg) { return new Judge(true, "MEDIUM", msg); }
    private static Judge high(String msg) { return new Judge(true, "HIGH", msg); }
}
