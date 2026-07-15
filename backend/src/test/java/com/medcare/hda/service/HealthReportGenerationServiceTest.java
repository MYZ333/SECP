package com.medcare.hda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.medcare.hda.dto.HealthReportDetailVO;
import com.medcare.hda.dto.HealthReportGenerateRequest;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthReportGenerationServiceTest {

    private final HealthMetricService metricService = mock(HealthMetricService.class);
    private final HealthProfileService profileService = mock(HealthProfileService.class);
    private final HealthReportService reportService = mock(HealthReportService.class);
    private final HealthReportAnalyzer analyzer = mock(HealthReportAnalyzer.class);
    private final HealthReportNarrativeService narrativeService = mock(HealthReportNarrativeService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HealthReportGenerationService service = new HealthReportGenerationService(
            metricService, profileService, reportService, analyzer, narrativeService, objectMapper);

    @Test
    void rejectsUnsupportedRangeBeforeReadingData() {
        HealthReportGenerateRequest request = new HealthReportGenerateRequest();
        request.setRangeDays(31);

        assertThatThrownBy(() -> service.generate(9L, request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("7、30 或 90");
    }

    @Test
    void persistsRuleResultAndReturnsGeneratedId() {
        when(reportService.getOne(any())).thenReturn(null);
        HealthMetric metric = new HealthMetric();
        metric.setMetricType("HEART_RATE"); metric.setMetricValue(72.0); metric.setMeasureTime(LocalDateTime.now());
        when(metricService.list(any(Wrapper.class))).thenReturn(List.of(metric));
        when(profileService.getOne(any())).thenReturn(null);
        HealthReportDetailVO analyzed = HealthReportDetailVO.builder().riskLevel("INSUFFICIENT")
                .algorithmVersion("2.0.0").dataCount(1).dataQuality(1.0).metrics(List.of()).build();
        when(analyzer.analyze(any(), any(), any(), any())).thenReturn(analyzed);
        HealthReportDetailVO.Narrative narrative = HealthReportDetailVO.Narrative.builder()
                .summary("数据不足。").findings(List.of()).recommendations(List.of("继续记录。"))
                .disclaimer("仅供健康管理。").build();
        when(narrativeService.create(analyzed, true))
                .thenReturn(new HealthReportNarrativeService.NarrativeResult(narrative, false));
        when(reportService.save(any())).thenAnswer(invocation -> {
            HealthReport report = invocation.getArgument(0); report.setId(88L); return true;
        });

        HealthReportDetailVO result = service.generate(9L, new HealthReportGenerateRequest());

        assertThat(result.getId()).isEqualTo(88L);
        assertThat(result.getGenerationMode()).isEqualTo("RULE");
        ArgumentCaptor<HealthReport> reportCaptor = ArgumentCaptor.forClass(HealthReport.class);
        verify(reportService).save(reportCaptor.capture());
        HealthReport saved = reportCaptor.getValue();
        assertThat(saved.getAlgorithmVersion()).isEqualTo("2.0.0");
        assertThat(saved.getStructuredResult()).contains("INSUFFICIENT");
        verify(reportService).updateById(saved);
    }
}
