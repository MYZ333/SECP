package com.medcare.hda.controller;

import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.DoctorRegisterDTO;
import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.RsaCryptoService;
import com.medcare.hda.service.AuthService;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Doctor auth", description = "Doctor login and registration")
@RestController
@RequestMapping("/api/doctor-auth")
@RequiredArgsConstructor
public class DoctorAuthController {

    private final UserService userService;
    private final DoctorService doctorService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RsaCryptoService rsaCryptoService;

    @Operation(summary = "Doctor login")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("Login success", authService.doctorLogin(dto));
    }

    @Operation(summary = "Doctor registration, pending admin approval")
    @PostMapping("/register")
    public Result<Doctor> register(@Valid @RequestBody DoctorRegisterDTO dto) {
        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        String rawPassword = rsaCryptoService.resolvePassword(dto.getPassword());
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 32) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Password length must be 6-32 characters");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(dto.getName());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userService.save(user);
        userService.assignRole(user.getId(), "DOCTOR");

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
        return Result.success("Registration submitted, please wait for admin approval", doctor);
    }
}
