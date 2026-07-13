package com.medcare.hda.agent.core;

import org.springframework.stereotype.Service;

@Service
public class OutputSafetyService {
    private static final String DISCLAIMER = "\n\n以上内容用于健康信息与就医准备，不能替代医生诊断、处方或急救。";
    private static final String MEDICAL_REVIEW = "\n\n请尽快到正规医疗机构就医评估。";

    public String enforce(String output, RiskAssessment risk) {
        String safe = sanitizeDiagnosisLanguage(output == null ? "" : output.trim());
        return safe + completionSuffix(safe, risk);
    }

    /** 可用于流式窗口的幂等文本过滤。 */
    public String sanitizeDiagnosisLanguage(String output) {
        if (output == null || output.isEmpty()) return "";
        return output.replaceAll("(?i)(您|你)(已经)?(患有|得了|确诊为)",
                "$1的情况需要由医生进一步评估，不能仅凭线上描述确诊为");
    }

    /** 流结束时只返回尚缺少的安全尾注，避免为了安全校验而缓存整篇回答。 */
    public String completionSuffix(String safeOutput, RiskAssessment risk) {
        String output = safeOutput == null ? "" : safeOutput;
        StringBuilder suffix = new StringBuilder();
        if (("HIGH".equals(risk.level()) || "EMERGENCY".equals(risk.level())) && !output.contains("就医")) {
            suffix.append(MEDICAL_REVIEW);
        }
        if (!output.contains("不能替代") && !output.contains("不替代")) suffix.append(DISCLAIMER);
        return suffix.toString();
    }
}
