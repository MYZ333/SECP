package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.HealthReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康档案-健康报告")
@RestController
@RequestMapping("/api/health/report")
@RequiredArgsConstructor
public class HealthReportController {

    private final HealthReportService service;

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

    private void checkOwner(HealthReport entity) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!entity.getUserId().equals(SecurityUtil.getUserId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
