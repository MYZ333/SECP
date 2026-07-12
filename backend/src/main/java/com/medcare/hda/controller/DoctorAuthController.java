package com.medcare.hda.controller;

import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.DoctorRegisterDTO;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.RsaCryptoService;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "医生认证", description = "医生注册 / 等待管理员审核")
@RestController
@RequestMapping("/api/doctor-auth")
@RequiredArgsConstructor
public class DoctorAuthController {

    private final UserService userService;
    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;
    private final RsaCryptoService rsaCryptoService;

    @Operation(summary = "医生注册，提交后等待管理员审核")
    @PostMapping("/register")
    public Result<Doctor> register(@Valid @RequestBody DoctorRegisterDTO dto) {
        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        String rawPassword = rsaCryptoService.resolvePassword(dto.getPassword());
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 32) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码长度为 6-32 位");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole("DOCTOR");
        user.setPoints(0);
        user.setStatus(1);
        user.setGender(0);
        userService.save(user);

        Doctor doctor = new Doctor();
        doctor.setUserId(user.getId());
        doctor.setName(dto.getName());
        doctor.setPhone(dto.getPhone());
        doctor.setHospital(dto.getHospital());
        doctor.setDepartment(dto.getDepartment());
        doctor.setTitle(dto.getTitle());
        doctor.setSpeciality(dto.getSpeciality());
        doctor.setIntroduction(dto.getIntroduction());
        doctor.setStatus(0);
        doctor.setAuditStatus("PENDING");
        doctorService.save(doctor);
        return Result.success("注册申请已提交，请等待管理员审核", doctor);
    }
}
