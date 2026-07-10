package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.AiChatService;
import com.medcare.hda.service.HealthAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康预警", description = "AI模块-健康预警")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final HealthAlertService service;
    private final AiChatService aiChatService;

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

    @Operation(summary = "生成健康预警(AI分析, 骨架占位)")
    @PostMapping("/generate")
    public Result<HealthAlert> generate() {
        Long userId = SecurityUtil.getUserId();
        String content = aiChatService.analyzeHealthRisk(userId);
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setLevel("LOW");
        alert.setAlertType("AI分析");
        alert.setContent(content);
        alert.setReadFlag(0);
        service.save(alert);
        return Result.success("已生成", alert);
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
