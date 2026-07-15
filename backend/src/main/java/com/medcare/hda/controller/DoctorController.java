package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "医生专家库", description = "AI模块-医生专家库查询")
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService service;

    @Operation(summary = "专家列表(分页; 科室/职称筛选; 关键词匹配姓名/医院/擅长)")
    @GetMapping("/page")
    public Result<PageResult<Doctor>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String department,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String keyword) {
        var page = service.page(new Page<>(pageNum, pageSize),
                Wrappers.<Doctor>lambdaQuery()
                        .eq(Doctor::getStatus, 1)
                        .eq(StringUtils.hasText(department), Doctor::getDepartment, department)
                        .eq(StringUtils.hasText(title), Doctor::getTitle, title)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Doctor::getName, keyword)
                                .or().like(Doctor::getHospital, keyword)
                                .or().like(Doctor::getSpeciality, keyword))
                        .orderByDesc(Doctor::getCreateTime));
        service.populateRatingStats(page.getRecords());
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "科室列表(库中实际存在的科室, 去重)")
    @GetMapping("/departments")
    public Result<List<String>> departments() {
        return Result.success(service.listActiveDepartments());
    }

    @Operation(summary = "专家详情")
    @GetMapping("/{id}")
    public Result<Doctor> get(@PathVariable Long id) {
        Doctor doctor = service.getCachedById(id);
        service.populateRatingStats(doctor);
        return Result.success(doctor);
    }
}
