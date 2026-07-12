package com.medcare.hda.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.User;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.UserService;
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
    private final UserService userService;

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
        if (doctor.getStatus() == null) doctor.setStatus(1);
        if (doctor.getAuditStatus() == null) doctor.setAuditStatus("APPROVED");
        service.save(doctor);
        return Result.success("新增成功", doctor);
    }

    @Operation(summary = "修改专家")
    @PutMapping
    public Result<Doctor> update(@RequestBody Doctor doctor) {
        service.updateById(doctor);
        return Result.success("修改成功", doctor);
    }

    @Operation(summary = "审核医生注册申请")
    @PutMapping("/{id}/audit")
    public Result<Doctor> audit(@PathVariable Long id, @RequestParam boolean approved) {
        Doctor doctor = service.getById(id);
        if (doctor == null) {
            return Result.fail("医生不存在");
        }
        doctor.setAuditStatus(approved ? "APPROVED" : "REJECTED");
        doctor.setStatus(approved ? 1 : 0);
        service.updateById(doctor);
        if (doctor.getUserId() != null) {
            User user = userService.getById(doctor.getUserId());
            if (user != null) {
                user.setStatus(approved ? 0 : 1);
                userService.updateById(user);
            }
        }
        return Result.success(approved ? "审核通过" : "已拒绝申请", doctor);
    }

    @Operation(summary = "删除专家")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success("删除成功", null);
    }
}
