package com.medcare.hda.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-医生专家维护")
@RestController
@RequestMapping("/api/admin/doctor")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final DoctorService service;

    @Operation(summary = "专家列表(分页)")
    @GetMapping("/page")
    public Result<PageResult<Doctor>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(pageNum, pageSize))));
    }

    @Operation(summary = "新增专家")
    @PostMapping
    public Result<Doctor> create(@RequestBody Doctor doctor) {
        doctor.setId(null);
        service.save(doctor);
        return Result.success("新增成功", doctor);
    }

    @Operation(summary = "修改专家")
    @PutMapping
    public Result<Doctor> update(@RequestBody Doctor doctor) {
        service.updateById(doctor);
        return Result.success("修改成功", doctor);
    }

    @Operation(summary = "删除专家")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success("删除成功", null);
    }
}
