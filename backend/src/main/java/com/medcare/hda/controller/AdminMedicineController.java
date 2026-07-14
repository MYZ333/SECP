package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.Medicine;
import com.medcare.hda.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-药品库")
@RestController
@RequestMapping("/api/admin/medicine")
@RequiredArgsConstructor
public class AdminMedicineController {

    private final MedicineService service;

    @Operation(summary = "药品列表")
    @GetMapping("/page")
    public Result<PageResult<Medicine>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        var query = Wrappers.<Medicine>lambdaQuery()
                .eq(status != null, Medicine::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Medicine::getName, keyword)
                        .or().like(Medicine::getGenericName, keyword)
                        .or().like(Medicine::getBrandName, keyword)
                        .or().like(Medicine::getCategory, keyword))
                .orderByDesc(Medicine::getUpdateTime)
                .orderByDesc(Medicine::getId);
        return Result.success(PageResult.of(service.page(new Page<>(pageNum, pageSize), query)));
    }

    @Operation(summary = "新增药品")
    @PostMapping
    public Result<Medicine> create(@RequestBody Medicine medicine) {
        medicine.setId(null);
        normalize(medicine);
        service.save(medicine);
        return Result.success("新增成功", medicine);
    }

    @Operation(summary = "修改药品")
    @PutMapping
    public Result<Medicine> update(@RequestBody Medicine medicine) {
        normalize(medicine);
        service.updateById(medicine);
        return Result.success("修改成功", medicine);
    }

    @Operation(summary = "删除药品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    private void normalize(Medicine medicine) {
        if (medicine.getStatus() == null) medicine.setStatus(1);
        if (medicine.getRequiresOffline() == null) medicine.setRequiresOffline(0);
        if (medicine.getDefaultDurationDays() == null) medicine.setDefaultDurationDays(0);
        if (medicine.getMaxDurationDays() == null) medicine.setMaxDurationDays(0);
    }
}
