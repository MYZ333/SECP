package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.MedicalRecord;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康档案-就诊/用药记录")
@RestController
@RequestMapping("/api/health/medical-record")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;

    @Operation(summary = "分页查询(当前用户)")
    @GetMapping("/page")
    public Result<PageResult<MedicalRecord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        Long userId = SecurityUtil.getUserId();
        IPage<MedicalRecord> page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<MedicalRecord>lambdaQuery()
                        .eq(MedicalRecord::getUserId, userId)
                        .orderByDesc(MedicalRecord::getVisitDate));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<MedicalRecord> get(@PathVariable Long id) {
        MedicalRecord entity = service.getById(id);
        checkOwner(entity);
        return Result.success(entity);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<MedicalRecord> create(@RequestBody MedicalRecord entity) {
        entity.setId(null);
        entity.setUserId(SecurityUtil.getUserId());
        service.save(entity);
        return Result.success("新增成功", entity);
    }

    @Operation(summary = "修改")
    @PutMapping
    public Result<MedicalRecord> update(@RequestBody MedicalRecord entity) {
        MedicalRecord exist = service.getById(entity.getId());
        checkOwner(exist);
        entity.setUserId(SecurityUtil.getUserId());
        service.updateById(entity);
        return Result.success("修改成功", entity);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        MedicalRecord exist = service.getById(id);
        checkOwner(exist);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    private void checkOwner(MedicalRecord entity) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!entity.getUserId().equals(SecurityUtil.getUserId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
