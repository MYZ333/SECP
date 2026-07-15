package com.medcare.hda.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.annotation.DistributedLock;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.HealthReportDetailVO;
import com.medcare.hda.dto.HealthReportGenerateRequest;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 健康报告 v2 生成编排：快照查询、规则分析、AI/模板文案和持久化。 */
@Service
@RequiredArgsConstructor
public class HealthReportGenerationService {

    public static final String ALGORITHM_VERSION = "2.0.0";
    private static final Set<Integer> ALLOWED_RANGES = Set.of(7, 30, 90);

    private final HealthMetricService metricService;
    private final HealthProfileService profileService;
    private final HealthReportService reportService;
    private final HealthReportAnalyzer analyzer;
    private final HealthReportNarrativeService narrativeService;
    private final ObjectMapper objectMapper;

    @Transactional
    @DistributedLock(key = "'health-report:' + #userId", waitSeconds = 1, leaseSeconds = 20,
            message = "健康报告正在生成，请勿重复提交")
    public HealthReportDetailVO generate(Long userId, HealthReportGenerateRequest request) {
        int rangeDays = request == null || request.getRangeDays() == null ? 30 : request.getRangeDays();
        if (!ALLOWED_RANGES.contains(rangeDays)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "统计周期仅支持 7、30 或 90 天");
        }
        boolean useAi = request == null || !Boolean.FALSE.equals(request.getUseAiNarrative());
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime periodStart = generatedAt.minusDays(rangeDays);

        HealthReport recent = findRecentEquivalent(userId, periodStart, generatedAt);
        if (recent != null) return detail(recent);

        List<HealthMetric> metrics = metricService.list(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId)
                .ge(HealthMetric::getMeasureTime, periodStart)
                .le(HealthMetric::getMeasureTime, generatedAt)
                .orderByAsc(HealthMetric::getMeasureTime));
        if (metrics.isEmpty() || metrics.stream().noneMatch(m -> m.getMetricValue() != null)) {
            throw new BusinessException("所选周期内暂无有效体征记录，请先在【体征数据】中记录后再生成报告");
        }
        HealthProfile profile = profileService.getOne(Wrappers.<HealthProfile>lambdaQuery()
                .eq(HealthProfile::getUserId, userId));
        HealthReportDetailVO detail = analyzer.analyze(metrics, profile, periodStart, generatedAt);
        HealthReportNarrativeService.NarrativeResult narrativeResult = narrativeService.create(detail, useAi);
        detail.setNarrative(narrativeResult.narrative());
        detail.setGenerationMode(narrativeResult.aiUsed() ? "RULE_AI" : "RULE");
        detail.setTitle("健康分析报告 " + LocalDate.now());
        detail.setReportType("AI");
        detail.setReportDate(LocalDate.now());
        detail.setContent(renderContent(detail));

        HealthReport report = new HealthReport();
        report.setUserId(userId);
        report.setTitle(detail.getTitle());
        report.setReportType("AI");
        report.setReportDate(detail.getReportDate());
        report.setContent(detail.getContent());
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(generatedAt);
        report.setRiskLevel(detail.getRiskLevel());
        report.setAlgorithmVersion(ALGORITHM_VERSION);
        report.setGenerationMode(detail.getGenerationMode());
        report.setDataCount(detail.getDataCount());
        report.setDataQuality(detail.getDataQuality());
        report.setStructuredResult(writeJson(detail));
        reportService.save(report);

        detail.setId(report.getId());
        report.setStructuredResult(writeJson(detail));
        reportService.updateById(report);
        return detail;
    }

    public HealthReportDetailVO detail(HealthReport report) {
        if (report == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (report.getStructuredResult() == null || report.getStructuredResult().isBlank()) {
            return HealthReportDetailVO.builder().id(report.getId()).title(report.getTitle())
                    .reportType(report.getReportType()).reportDate(report.getReportDate())
                    .legacy(true).content(report.getContent()).build();
        }
        try {
            HealthReportDetailVO detail = objectMapper.readValue(report.getStructuredResult(), HealthReportDetailVO.class);
            detail.setId(report.getId());
            detail.setLegacy(false);
            return detail;
        } catch (JsonProcessingException e) {
            throw new BusinessException("报告结构化数据损坏，无法展示详情");
        }
    }

    private HealthReport findRecentEquivalent(Long userId, LocalDateTime periodStart, LocalDateTime generatedAt) {
        return reportService.getOne(Wrappers.<HealthReport>lambdaQuery()
                .eq(HealthReport::getUserId, userId)
                .eq(HealthReport::getAlgorithmVersion, ALGORITHM_VERSION)
                .ge(HealthReport::getCreateTime, generatedAt.minusSeconds(60))
                .between(HealthReport::getPeriodStart, periodStart.minusMinutes(1), periodStart.plusMinutes(1))
                .orderByDesc(HealthReport::getCreateTime)
                .last("LIMIT 1"));
    }

    private String writeJson(HealthReportDetailVO detail) {
        try { return objectMapper.writeValueAsString(detail); }
        catch (JsonProcessingException e) { throw new BusinessException("报告结构化数据序列化失败"); }
    }

    private String renderContent(HealthReportDetailVO detail) {
        StringBuilder text = new StringBuilder(detail.getNarrative().getSummary()).append("\n\n主要发现：\n");
        detail.getNarrative().getFindings().forEach(item -> text.append("- ").append(item).append('\n'));
        text.append("\n建议：\n");
        detail.getNarrative().getRecommendations().forEach(item -> text.append("- ").append(item).append('\n'));
        return text.append("\n").append(detail.getNarrative().getDisclaimer()).toString();
    }
}
