package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.MetricRules;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.HealthAlertService;
import com.medcare.hda.service.HealthMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "健康预警", description = "健康预警(阈值规则引擎)")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final HealthAlertService service;
    private final HealthMetricService metricService;

    @Operation(summary = "我的健康预警(分页)")
    @GetMapping("/page")
    public Result<PageResult<HealthAlert>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize) {
        Long userId = SecurityUtil.getUserId();
        var page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<HealthAlert>lambdaQuery()
                        .eq(HealthAlert::getUserId, userId)
                        .orderByDesc(HealthAlert::getCreateTime));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "生成健康预警(基于近7日异常体征的阈值规则分析)")
    @PostMapping("/generate")
    public Result<List<HealthAlert>> generate() {
        Long userId = SecurityUtil.getUserId();
        // 拉取近 7 日体征记录，逐条按阈值规则复判
        List<HealthMetric> metrics = metricService.list(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId)
                .ge(HealthMetric::getMeasureTime, LocalDateTime.now().minusDays(7))
                .orderByDesc(HealthMetric::getMeasureTime));

        // 按指标类型聚合：取最新一条异常 + 统计异常次数，级别取最严重
        Map<String, List<MetricRules.Judge>> abnormalByType = new LinkedHashMap<>();
        Map<String, HealthMetric> latestByType = new LinkedHashMap<>();
        for (HealthMetric m : metrics) {
            MetricRules.Judge j = MetricRules.judge(m);
            if (j.abnormal()) {
                abnormalByType.computeIfAbsent(m.getMetricType(), k -> new ArrayList<>()).add(j);
                latestByType.putIfAbsent(m.getMetricType(), m);
            }
        }

        List<HealthAlert> created = new ArrayList<>();
        abnormalByType.forEach((type, judges) -> {
            String level = judges.stream().anyMatch(j -> "HIGH".equals(j.level())) ? "HIGH" : "MEDIUM";
            MetricRules.Judge latest = MetricRules.judge(latestByType.get(type));
            String content = String.format("近7日共 %d 次%s异常。最近一次：%s。",
                    judges.size(), MetricRules.typeName(type), latest.message());
            HealthAlert alert = new HealthAlert();
            alert.setUserId(userId);
            alert.setLevel(level);
            alert.setAlertType(MetricRules.typeName(type) + "异常");
            alert.setContent(content);
            alert.setReadFlag(0);
            service.save(alert);
            created.add(alert);
        });

        String msg = created.isEmpty()
                ? "分析完成：近7日 " + metrics.size() + " 条体征记录均正常，未生成预警"
                : "分析完成：生成 " + created.size() + " 条预警";
        return Result.success(msg, created);
    }

    @Operation(summary = "标记预警为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        HealthAlert alert = service.getById(id);
        if (alert != null && alert.getUserId().equals(SecurityUtil.getUserId())) {
            alert.setReadFlag(1);
            service.updateById(alert);
        }
        return Result.success("操作成功", null);
    }
}
