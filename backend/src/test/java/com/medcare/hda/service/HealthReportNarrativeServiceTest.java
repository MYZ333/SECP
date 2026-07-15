package com.medcare.hda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.dto.HealthReportDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;

class HealthReportNarrativeServiceTest {

    @Test
    void usesRuleTemplateWhenAiIsDisabled() {
        HealthReportNarrativeService service = new HealthReportNarrativeService(mock(ChatClient.class), new ObjectMapper());
        ReflectionTestUtils.setField(service, "aiEnabled", false);
        ReflectionTestUtils.setField(service, "apiKey", "unused");
        HealthReportDetailVO detail = HealthReportDetailVO.builder()
                .riskLevel("WARNING").dataCount(4)
                .metrics(List.of(HealthReportDetailVO.MetricAnalysis.builder()
                        .metricName("心率").validCount(4).abnormalRate(0.5).trend("UP").build()))
                .build();

        HealthReportNarrativeService.NarrativeResult result = service.create(detail, true);

        assertThat(result.aiUsed()).isFalse();
        assertThat(result.narrative().getSummary()).contains("建议关注").contains("4 条");
        assertThat(result.narrative().getRecommendations()).allMatch(item -> !item.contains("调整剂量"));
        assertThat(result.narrative().getDisclaimer()).contains("不构成疾病诊断");
    }

    @Test
    void acceptsCompliantStructuredAiNarrative() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(
                "{\"riskLevel\":\"NORMAL\",\"summary\":\"记录整体平稳。\",\"findings\":[\"心率趋势平稳。\"],\"recommendations\":[\"继续规律测量。\"],\"disclaimer\":\"\"}");
        HealthReportNarrativeService service = enabledService(client);

        HealthReportNarrativeService.NarrativeResult result = service.create(detail("NORMAL"), true);

        assertThat(result.aiUsed()).isTrue();
        assertThat(result.narrative().getSummary()).isEqualTo("记录整体平稳。");
    }

    @Test
    void rejectsChangedRiskFictitiousMetricAndMedicationLanguage() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(
                "{\"riskLevel\":\"HIGH\",\"summary\":\"血糖已经确诊异常。\",\"findings\":[],\"recommendations\":[\"建议调整剂量。\"],\"disclaimer\":\"\"}");
        HealthReportNarrativeService service = enabledService(client);

        HealthReportNarrativeService.NarrativeResult result = service.create(detail("NORMAL"), true);

        assertThat(result.aiUsed()).isFalse();
        assertThat(result.narrative().getSummary()).doesNotContain("确诊");
    }

    @Test
    void timesOutAndFallsBackWithoutFailingReport() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("{}");
        HealthReportNarrativeService service = enabledService(client);
        ReflectionTestUtils.setField(service, "timeoutSeconds", 0L);

        HealthReportNarrativeService.NarrativeResult result = service.create(detail("NORMAL"), true);

        assertThat(result.aiUsed()).isFalse();
        assertThat(result.narrative().getDisclaimer()).isNotBlank();
    }

    private HealthReportNarrativeService enabledService(ChatClient client) {
        HealthReportNarrativeService service = new HealthReportNarrativeService(client, new ObjectMapper());
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 1L);
        return service;
    }

    private HealthReportDetailVO detail(String risk) {
        return HealthReportDetailVO.builder().riskLevel(risk).dataCount(3)
                .metrics(List.of(HealthReportDetailVO.MetricAnalysis.builder()
                        .metricName("心率").validCount(3).abnormalRate(0.0).trend("STABLE").build()))
                .build();
    }
}
