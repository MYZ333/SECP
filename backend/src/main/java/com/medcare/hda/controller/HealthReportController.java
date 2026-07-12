package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.MetricRules;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.HealthMetricService;
import com.medcare.hda.service.HealthReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "健康档案-健康报告")
@RestController
@RequestMapping("/api/health/report")
@RequiredArgsConstructor
public class HealthReportController {

    private final HealthReportService service;
    private final HealthMetricService metricService;

    @Operation(summary = "分页查询(当前用户)")
    @GetMapping("/page")
    public Result<PageResult<HealthReport>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        Long userId = SecurityUtil.getUserId();
        IPage<HealthReport> page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<HealthReport>lambdaQuery()
                        .eq(HealthReport::getUserId, userId)
                        .orderByDesc(HealthReport::getReportDate));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<HealthReport> get(@PathVariable Long id) {
        HealthReport entity = service.getById(id);
        checkOwner(entity);
        return Result.success(entity);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<HealthReport> create(@RequestBody HealthReport entity) {
        entity.setId(null);
        entity.setUserId(SecurityUtil.getUserId());
        service.save(entity);
        return Result.success("新增成功", entity);
    }

    @Operation(summary = "修改")
    @PutMapping
    public Result<HealthReport> update(@RequestBody HealthReport entity) {
        HealthReport exist = service.getById(entity.getId());
        checkOwner(exist);
        entity.setUserId(SecurityUtil.getUserId());
        service.updateById(entity);
        return Result.success("修改成功", entity);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        HealthReport exist = service.getById(id);
        checkOwner(exist);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "一键生成健康报告(近30天体征统计分析)")
    @PostMapping("/generate")
    public Result<HealthReport> generate() {
        Long userId = SecurityUtil.getUserId();
        List<HealthMetric> metrics = metricService.list(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId)
                .ge(HealthMetric::getMeasureTime, LocalDateTime.now().minusDays(30))
                .orderByAsc(HealthMetric::getMeasureTime));
        if (metrics.isEmpty()) {
            throw new BusinessException("近30天暂无体征记录，请先在【体征数据】中记录后再生成报告");
        }

        Map<String, List<HealthMetric>> byType = metrics.stream()
                .collect(Collectors.groupingBy(HealthMetric::getMetricType,
                        java.util.LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("统计周期：%s 至 %s，共 %d 条体征记录。%n%n",
                LocalDate.now().minusDays(30), LocalDate.now(), metrics.size()));
        int totalAbnormal = 0;
        for (var e : byType.entrySet()) {
            List<HealthMetric> list = e.getValue();
            String name = MetricRules.typeName(e.getKey());
            double avg1 = list.stream().mapToDouble(HealthMetric::getMetricValue).average().orElse(0);
            double max1 = list.stream().mapToDouble(HealthMetric::getMetricValue).max().orElse(0);
            double min1 = list.stream().mapToDouble(HealthMetric::getMetricValue).min().orElse(0);
            long abnormal = list.stream().filter(m -> MetricRules.judge(m).abnormal()).count();
            totalAbnormal += (int) abnormal;
            sb.append(String.format("【%s】记录 %d 次，均值 %.1f，最高 %.1f，最低 %.1f",
                    name, list.size(), avg1, max1, min1));
            if ("BLOOD_PRESSURE".equals(e.getKey())) {
                double avg2 = list.stream().filter(m -> m.getMetricValue2() != null)
                        .mapToDouble(HealthMetric::getMetricValue2).average().orElse(0);
                sb.append(String.format("（舒张压均值 %.1f）", avg2));
            }
            sb.append(String.format("，异常 %d 次。%n", abnormal));
        }
        sb.append(String.format("%n总体结论：30 天内共 %d 次异常记录，", totalAbnormal));
        sb.append(totalAbnormal == 0 ? "各项指标平稳，请继续保持规律作息与测量习惯。"
                : totalAbnormal <= 3 ? "存在少量异常，建议增加测量频率并注意饮食作息。"
                : "异常次数偏多，建议前往医院进行系统检查，并到【健康预警】生成风险提示。");

        HealthReport report = new HealthReport();
        report.setUserId(userId);
        report.setTitle("健康分析报告 " + LocalDate.now());
        report.setReportType("AI");
        report.setReportDate(LocalDate.now());
        report.setContent(sb.toString());
        service.save(report);
        return Result.success("报告已生成", report);
    }

    private void checkOwner(HealthReport entity) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!entity.getUserId().equals(SecurityUtil.getUserId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
