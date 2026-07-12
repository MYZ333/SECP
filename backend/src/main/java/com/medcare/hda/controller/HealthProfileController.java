package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.HealthProfileService;
import com.medcare.hda.service.PointService;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康档案-基本信息")
@RestController
@RequestMapping("/api/health/profile")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService service;
    private final PointService pointService;

    @Operation(summary = "获取当前用户健康基本信息")
    @GetMapping
    public Result<HealthProfile> get() {
        Long userId = SecurityUtil.getUserId();
        HealthProfile profile = service.getOne(
                Wrappers.<HealthProfile>lambdaQuery().eq(HealthProfile::getUserId, userId));
        return Result.success(profile);
    }

    @Operation(summary = "保存/更新健康基本信息")
    @PostMapping
    public Result<HealthProfile> save(@RequestBody HealthProfile profile) {
        Long userId = SecurityUtil.getUserId();
        HealthProfile exist = service.getOne(
                Wrappers.<HealthProfile>lambdaQuery().eq(HealthProfile::getUserId, userId));
        profile.setUserId(userId);
        if (exist != null) {
            profile.setId(exist.getId());
            service.updateById(profile);
        } else {
            service.save(profile);
        }
        // 积分任务: 关键信息填写完整后标记为"待领取"（到积分中心手动领取，一次性）
        if (isComplete(profile)) {
            pointService.markTaskReady(userId, "PROFILE");
        }
        return Result.success("保存成功", profile);
    }

    /** 判断档案关键信息是否填写完整 */
    private boolean isComplete(HealthProfile p) {
        return p.getHeight() != null && p.getWeight() != null
                && StringUtils.hasText(p.getBloodType())
                && StringUtils.hasText(p.getEmergencyContact())
                && StringUtils.hasText(p.getEmergencyPhone());
    }
}
