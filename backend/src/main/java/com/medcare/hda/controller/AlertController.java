package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.dto.AlertActionDTO;
import com.medcare.hda.dto.AlertAnalysisVO;
import com.medcare.hda.dto.AlertSummaryVO;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.AlertAnalysisService;
import com.medcare.hda.service.HealthAlertService;
import com.medcare.hda.service.HealthAlertLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "健康预警", description = "健康预警(阈值规则引擎)")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final HealthAlertService service;
    private final AlertAnalysisService analysisService;
    private final HealthAlertLifecycleService lifecycleService;

    @Operation(summary = "我的健康预警(分页)")
    @GetMapping("/page")
    public Result<PageResult<HealthAlert>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String level,
                                                @RequestParam(required = false) Integer readFlag,
                                                @RequestParam(required = false) String status) {
        Long userId = SecurityUtil.getUserId();
        boolean validLevel = Set.of("LOW", "MEDIUM", "HIGH").contains(level == null ? "" : level);
        boolean validReadFlag = readFlag != null && (readFlag == 0 || readFlag == 1);
        boolean validStatus = Set.of("ACTIVE", "OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "IGNORED")
                .contains(status == null ? "" : status);
        boolean activeStatus = "ACTIVE".equals(status);
        var page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<HealthAlert>lambdaQuery()
                        .eq(HealthAlert::getUserId, userId)
                        .eq(validLevel, HealthAlert::getLevel, level)
                        .eq(validReadFlag, HealthAlert::getReadFlag, readFlag)
                        .eq(validStatus && !activeStatus, HealthAlert::getStatus, status)
                        .and(activeStatus, wrapper -> wrapper.in(HealthAlert::getStatus, "OPEN", "ACKNOWLEDGED", "IN_PROGRESS")
                                .or().isNull(HealthAlert::getStatus))
                        .orderByDesc(HealthAlert::getCreateTime));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "健康预警统计")
    @GetMapping("/summary")
    public Result<AlertSummaryVO> summary() {
        Long userId = SecurityUtil.getUserId();
        long total = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId));
        long active = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId)
                .and(wrapper -> wrapper.in(HealthAlert::getStatus, "OPEN", "ACKNOWLEDGED", "IN_PROGRESS")
                        .or().isNull(HealthAlert::getStatus)));
        long unread = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getReadFlag, 0));
        long highUnread = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getReadFlag, 0)
                .eq(HealthAlert::getLevel, "HIGH"));
        long mediumUnread = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getReadFlag, 0)
                .eq(HealthAlert::getLevel, "MEDIUM"));
        long open = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId)
                .and(wrapper -> wrapper.eq(HealthAlert::getStatus, "OPEN").or().isNull(HealthAlert::getStatus)));
        long inProgress = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getStatus, "IN_PROGRESS"));
        long highActive = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getLevel, "HIGH")
                .and(wrapper -> wrapper.in(HealthAlert::getStatus, "OPEN", "ACKNOWLEDGED", "IN_PROGRESS")
                        .or().isNull(HealthAlert::getStatus)));
        long resolved = service.count(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).eq(HealthAlert::getStatus, "RESOLVED"));
        HealthAlert latest = service.getOne(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId)
                .orderByDesc(HealthAlert::getLastTriggerTime)
                .orderByDesc(HealthAlert::getCreateTime)
                .last("LIMIT 1"), false);
        return Result.success(new AlertSummaryVO(total, active, unread, highUnread, mediumUnread,
                open, inProgress, highActive, resolved,
                latest == null ? null : (latest.getLastTriggerTime() == null ? latest.getCreateTime() : latest.getLastTriggerTime())));
    }

    @Operation(summary = "预览近7日体征阈值分析，不生成预警")
    @GetMapping("/preview")
    public Result<AlertAnalysisVO> preview() {
        AlertAnalysisVO result = analysisService.preview(SecurityUtil.getUserId());
        return Result.success(result.getConclusion(), result);
    }

    @Operation(summary = "生成健康预警(基于近7日异常体征的阈值规则分析)")
    @PostMapping("/generate")
    public Result<AlertAnalysisVO> generate() {
        AlertAnalysisVO result = analysisService.generate(SecurityUtil.getUserId());
        return Result.success(result.getConclusion(), result);
    }

    @Operation(summary = "标记预警为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        lifecycleService.acknowledge(id, SecurityUtil.getUserId());
        return Result.success("操作成功", null);
    }

    @Operation(summary = "开始处理预警并关联咨询渠道")
    @PutMapping("/{id}/in-progress")
    public Result<HealthAlert> startHandling(@PathVariable Long id, @Valid @RequestBody AlertActionDTO action) {
        return Result.success("已进入处理", lifecycleService.startHandling(id, SecurityUtil.getUserId(), action));
    }

    @Operation(summary = "将预警标记为已解决")
    @PutMapping("/{id}/resolve")
    public Result<HealthAlert> resolve(@PathVariable Long id, @Valid @RequestBody(required = false) AlertActionDTO action) {
        return Result.success("预警已解决", lifecycleService.resolve(id, SecurityUtil.getUserId(), action));
    }

    @Operation(summary = "将误录或无效预警标记为已忽略")
    @PutMapping("/{id}/ignore")
    public Result<HealthAlert> ignore(@PathVariable Long id, @Valid @RequestBody(required = false) AlertActionDTO action) {
        return Result.success("预警已忽略", lifecycleService.ignore(id, SecurityUtil.getUserId(), action));
    }
}
