package com.medcare.hda.agent.core;

import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.AgentAuditRepository.HealthConsultTurn;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthConsultHandoffServiceTest {
    @Test
    void shouldCreateConciseDoctorHandoffFromCompletedHealthConsultation() {
        AgentAuditRepository auditRepository = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository stateRepository = mock(ClinicalIntakeStateRepository.class);
        HealthConsultHandoffService service = new HealthConsultHandoffService(auditRepository, stateRepository);
        String sessionId = "9dc7a153-111d-4fbb-aae4-31b62e9281ea";
        when(auditRepository.findTurnsForHandoff(7L, sessionId)).thenReturn(List.of(
                new HealthConsultTurn("前天发烧39℃，现在咳嗽", "请补充持续时间", "LOW"),
                new HealthConsultTurn("咳嗽两天，有咳痰", "建议复测体温并就医评估", "MEDIUM"),
                new HealthConsultTurn("需要", "已匹配医生", "MEDIUM")));
        when(stateRepository.findLatestCompletedForHandoff(7L, sessionId)).thenReturn(Optional.of(
                new ClinicalIntakeState(7L, sessionId, "episode", "COMPLETED", 2,
                        "前天发烧39℃，现在咳嗽", "发热为前天历史记录，目前咳嗽两天并有咳痰\n待回答问题：是否呼吸困难？",
                        List.of("前天体温39℃", "目前咳嗽两天", "有咳痰"), List.of())));

        String summary = service.summarize(7L, sessionId);

        assertTrue(summary.contains("【健康助手问诊摘要】"));
        assertTrue(summary.contains("患者主诉：前天发烧39℃，现在咳嗽"));
        assertTrue(summary.contains("目前咳嗽两天"));
        assertTrue(summary.contains("健康助手风险分级：中风险（MEDIUM）"));
        assertFalse(summary.contains("待回答问题"));
        assertFalse(summary.contains("；需要"));
    }

    @Test
    void shouldRemoveRawButtonAnswersAlreadyCoveredByStructuredFacts() {
        AgentAuditRepository auditRepository = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository stateRepository = mock(ClinicalIntakeStateRepository.class);
        HealthConsultHandoffService service = new HealthConsultHandoffService(auditRepository, stateRepository);
        String sessionId = "0ec7a153-111d-4fbb-aae4-31b62e9281ea";
        when(auditRepository.findTurnsForHandoff(7L, sessionId)).thenReturn(List.of(
                new HealthConsultTurn("我今天又感冒了", "请补充", "LOW"),
                new HealthConsultTurn("今天刚开始（24小时内）", "请补充", "LOW"),
                new HealthConsultTurn("38.5℃以下", "请补充", "LOW"),
                new HealthConsultTurn("有咳嗽或咳痰", "已完成问诊", "LOW"),
                new HealthConsultTurn("需要", "已匹配医生", "LOW")));
        when(stateRepository.findLatestCompletedForHandoff(7L, sessionId)).thenReturn(Optional.of(
                new ClinicalIntakeState(7L, sessionId, "episode", "COMPLETED", 4,
                        "我今天又感冒了", "今日出现发烧和畏寒，起病时间为24小时内，当前体温在38.5℃以下",
                        List.of("用户自述今日出现感冒症状，发烧/畏寒",
                                "起病时间：今天刚开始（24小时内）", "今天刚开始（24小时内）",
                                "当前体温：38.5℃以下", "38.5℃以下",
                                "有咳嗽、咳痰或喉咙痛", "有咳嗽或咳痰"), List.of())));

        String summary = service.summarize(7L, sessionId);

        assertTrue(summary.contains("起病时间：今天刚开始（24小时内）"));
        assertTrue(summary.contains("当前体温：38.5℃以下"));
        assertTrue(summary.contains("有咳嗽、咳痰或喉咙痛"));
        assertFalse(summary.contains("；今天刚开始（24小时内）"));
        assertFalse(summary.contains("；38.5℃以下"));
        assertFalse(summary.contains("；有咳嗽或咳痰"));
    }
}
