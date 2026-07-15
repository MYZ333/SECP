package com.medcare.hda.agent.core;

import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.AgentAuditRepository.HealthConsultTurn;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HealthConsultHandoffService {
    private static final Set<String> DOCTOR_OFFER_REPLIES = Set.of(
            "需要", "需要的", "要", "好的", "好", "可以", "可以的", "请帮我匹配", "帮我匹配");

    private final AgentAuditRepository auditRepository;
    private final ClinicalIntakeStateRepository intakeStateRepository;

    public String summarize(Long userId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "缺少健康助手会话信息");
        }
        List<HealthConsultTurn> turns = auditRepository.findTurnsForHandoff(userId, sessionId);
        if (turns.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到可转交的健康助手问诊记录");
        }

        ClinicalIntakeState state = intakeStateRepository.findLatestCompletedForHandoff(userId, sessionId).orElse(null);
        String chiefComplaint = state != null && StringUtils.hasText(state.initialQuestion())
                ? state.initialQuestion().trim() : clean(turns.getFirst().question(), 300);

        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (state != null && state.knownFacts() != null) {
            state.knownFacts().forEach(value -> addFact(facts, value));
        }
        List<String> turnFacts = turns.stream().map(HealthConsultTurn::question).map(value -> clean(value, 180))
                .filter(StringUtils::hasText)
                .filter(value -> !value.equals(chiefComplaint))
                .filter(value -> !isDoctorOfferReply(value))
                .toList();
        if (state == null) {
            turnFacts.forEach(value -> addFact(facts, value));
        } else if (!turnFacts.isEmpty()) {
            // 已完成状态包含此前各轮事实，只补入最后一次回答，避免把按钮原文再次拼到结构化事实后面。
            addFact(facts, turnFacts.getLast());
        }
        facts.remove(chiefComplaint);

        String clinicalSummary = state == null ? null : cleanClinicalSummary(state.clinicalSummary());
        String riskLevel = turns.reversed().stream().map(HealthConsultTurn::riskLevel)
                .filter(StringUtils::hasText).findFirst().orElse("未标记");

        StringBuilder summary = new StringBuilder("【健康助手问诊摘要】\n")
                .append("患者主诉：").append(chiefComplaint).append('\n');
        if (!facts.isEmpty()) {
            summary.append("患者补充：").append(String.join("；", facts.stream().limit(10).toList())).append('\n');
        }
        if (StringUtils.hasText(clinicalSummary) && !clinicalSummary.equals(chiefComplaint)) {
            summary.append("问诊整理：").append(clinicalSummary).append('\n');
        }
        summary.append("健康助手风险分级：").append(riskLabel(riskLevel)).append('\n')
                .append("以上内容根据患者在健康助手中的陈述整理，请医生接诊时进一步核实当前症状与病史。");
        return clean(summary.toString(), 2400);
    }

    private String cleanClinicalSummary(String value) {
        if (!StringUtils.hasText(value)) return null;
        return clean(value.replaceAll("(?m)^待回答问题：.*$", "").trim(), 800);
    }

    private String clean(String value, int maxLength) {
        if (value == null) return null;
        String cleaned = value.replaceAll("[\\t\\r]+", " ").replaceAll(" +", " ").trim();
        return cleaned.substring(0, Math.min(cleaned.length(), maxLength));
    }

    private void addFact(LinkedHashSet<String> facts, String rawFact) {
        String candidate = clean(rawFact, 180);
        if (!StringUtils.hasText(candidate)) return;
        String candidateKey = factKey(candidate);
        for (String existing : List.copyOf(facts)) {
            String existingKey = factKey(existing);
            if (existingKey.equals(candidateKey) || containsFact(existingKey, candidateKey)) return;
            if (containsFact(candidateKey, existingKey)) facts.remove(existing);
        }
        facts.add(candidate);
    }

    private boolean containsFact(String longer, String shorter) {
        return shorter.length() >= 5 && longer.contains(shorter);
    }

    private String factKey(String value) {
        return value.replaceAll("[\\s，。；;：:、（）()！？!?]", "")
                .replace("以及", "").replace("或者", "").replace("或", "").replace("和", "");
    }

    private boolean isDoctorOfferReply(String value) {
        return DOCTOR_OFFER_REPLIES.contains(value.replaceAll("[\\s，。！？!?]", ""));
    }

    private String riskLabel(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> "低风险（LOW）";
            case "MEDIUM" -> "中风险（MEDIUM）";
            case "HIGH" -> "高风险（HIGH）";
            case "EMERGENCY" -> "紧急（EMERGENCY）";
            default -> riskLevel;
        };
    }
}
