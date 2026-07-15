package com.medcare.hda.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.dto.HealthReportDetailVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** AI 仅负责把已确定的规则结论改写为自然语言；任何失败都返回规则模板。 */
@Slf4j
@Service
public class HealthReportNarrativeService {

    private static final String DISCLAIMER = "本报告仅用于个人健康数据管理，不构成疾病诊断、处方或用药调整依据；如有不适或持续异常，请及时咨询医生。";
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])[-+]?\\d+(?:\\.\\d+)?");
    private static final List<String> FORBIDDEN = List.of("确诊", "处方", "停药", "加量", "减量", "剂量", "替代医生", "无需就医");
    private static final Set<String> KNOWN_METRIC_NAMES = Set.of("血压", "血糖", "心率", "体温", "体重");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public HealthReportNarrativeService(
            @Qualifier("healthReportChatClient") ChatClient chatClient,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Value("${hda.health-report.ai.enabled:true}")
    private boolean aiEnabled;
    @Value("${hda.health-report.ai.timeout-seconds:10}")
    private long timeoutSeconds;
    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public NarrativeResult create(HealthReportDetailVO detail, boolean requested) {
        HealthReportDetailVO.Narrative fallback = fallback(detail);
        if (!requested || !aiEnabled || !StringUtils.hasText(apiKey)) {
            return new NarrativeResult(fallback, false);
        }
        try {
            String input = objectMapper.writeValueAsString(detail);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                            .system(systemPrompt())
                            .user(input)
                            .call()
                            .content());
            String raw;
            try {
                raw = future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
                throw e;
            }
            AiNarrative parsed = objectMapper.readValue(stripFence(raw), AiNarrative.class);
            validate(parsed, detail, input);
            return new NarrativeResult(HealthReportDetailVO.Narrative.builder()
                    .summary(parsed.summary)
                    .findings(parsed.findings == null ? List.of() : parsed.findings)
                    .recommendations(parsed.recommendations == null ? List.of() : parsed.recommendations)
                    .disclaimer(DISCLAIMER)
                    .build(), true);
        } catch (Exception e) {
            log.warn("健康报告 AI 文案生成或校验失败，已降级为规则模板: {}", e.getMessage());
            return new NarrativeResult(fallback, false);
        }
    }

    private String systemPrompt() {
        return """
                你是健康数据报告文案助手。唯一事实来源是用户提供的 JSON。
                规则风险等级不可修改；只描述测量值、趋势和规则结论，不诊断疾病，不提供处方，不建议停药或调整剂量。
                不得引入 JSON 中不存在的指标、病史、数值或身份信息。
                仅输出单个 JSON 对象，不要 Markdown：
                {"riskLevel":"与输入完全一致","summary":"一句摘要","findings":["发现"],"recommendations":["行动建议"],"disclaimer":"占位"}
                summary、findings、recommendations 应简洁、温和、可执行；不要重复免责声明。
                """;
    }

    private void validate(AiNarrative ai, HealthReportDetailVO detail, String input) {
        if (ai == null || !detail.getRiskLevel().equals(ai.riskLevel) || !StringUtils.hasText(ai.summary)) {
            throw new IllegalArgumentException("AI 风险等级或摘要不合法");
        }
        List<String> texts = new ArrayList<>();
        texts.add(ai.summary);
        if (ai.findings != null) texts.addAll(ai.findings);
        if (ai.recommendations != null) texts.addAll(ai.recommendations);
        String joined = String.join(" ", texts);
        for (String word : FORBIDDEN) {
            if (joined.contains(word)) throw new IllegalArgumentException("AI 包含禁止表述: " + word);
        }
        Set<String> allowedNames = detail.getMetrics().stream().map(HealthReportDetailVO.MetricAnalysis::getMetricName)
                .collect(java.util.stream.Collectors.toSet());
        for (String name : KNOWN_METRIC_NAMES) {
            if (joined.contains(name) && !allowedNames.contains(name)) {
                throw new IllegalArgumentException("AI 引入不存在的指标: " + name);
            }
        }
        Set<String> allowedNumbers = numbers(input);
        for (String number : numbers(joined)) {
            if (!allowedNumbers.contains(number)) throw new IllegalArgumentException("AI 引入不存在的数值: " + number);
        }
    }

    private Set<String> numbers(String text) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        Matcher matcher = NUMBER.matcher(text == null ? "" : text);
        while (matcher.find()) result.add(normalizeNumber(matcher.group()));
        return result;
    }

    private String normalizeNumber(String value) {
        try { return new java.math.BigDecimal(value).stripTrailingZeros().toPlainString(); }
        catch (NumberFormatException e) { return value.toLowerCase(Locale.ROOT); }
    }

    private String stripFence(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return value;
    }

    private HealthReportDetailVO.Narrative fallback(HealthReportDetailVO detail) {
        List<String> findings = detail.getMetrics().stream().map(metric ->
                String.format("%s共 %d 条有效记录，异常率 %.1f%%，趋势为%s。",
                        metric.getMetricName(), metric.getValidCount(), metric.getAbnormalRate() * 100,
                        trendName(metric.getTrend()))).toList();
        List<String> recommendations = switch (detail.getRiskLevel()) {
            case "HIGH" -> List.of("记录中出现高级别异常测量值，建议及时咨询医生；如伴明显不适，请及时就医。");
            case "WARNING" -> List.of("存在连续异常或较高异常率，建议规范复测并咨询医生。");
            case "ATTENTION" -> List.of("存在偶发异常，建议保持规律测量并关注后续变化。");
            case "INSUFFICIENT" -> List.of("当前数据不足，建议补充至少 3 次且跨 3 个测量日的记录。");
            default -> List.of("当前记录整体平稳，请继续保持规律作息和测量习惯。");
        };
        return HealthReportDetailVO.Narrative.builder()
                .summary("本周期综合关注等级为" + riskName(detail.getRiskLevel()) + "，共纳入 " + detail.getDataCount() + " 条有效记录。")
                .findings(findings)
                .recommendations(recommendations)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private String trendName(String trend) {
        return switch (trend == null ? "" : trend) {
            case "UP" -> "上升"; case "DOWN" -> "下降"; case "STABLE" -> "平稳"; default -> "数据不足";
        };
    }

    private String riskName(String risk) {
        return switch (risk == null ? "" : risk) {
            case "HIGH" -> "高度关注"; case "WARNING" -> "建议关注"; case "ATTENTION" -> "轻度关注";
            case "NORMAL" -> "正常"; default -> "数据不足";
        };
    }

    public record NarrativeResult(HealthReportDetailVO.Narrative narrative, boolean aiUsed) {}

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiNarrative {
        private String riskLevel;
        private String summary;
        private List<String> findings;
        private List<String> recommendations;
        private String disclaimer;
    }
}
