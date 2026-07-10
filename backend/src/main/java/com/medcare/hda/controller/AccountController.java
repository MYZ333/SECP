package com.medcare.hda.controller;

import com.medcare.hda.common.Result;
import com.medcare.hda.dto.ChangePasswordDTO;
import com.medcare.hda.dto.UpdateProfileDTO;
import com.medcare.hda.entity.User;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账户管理", description = "个人资料 / 账户安全")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<User> me() {
        User user = userService.getById(SecurityUtil.getUserId());
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success("修改成功", userService.updateProfile(SecurityUtil.getUserId(), dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(SecurityUtil.getUserId(), dto);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "注销账号")
    @DeleteMapping("/deactivate")
    public Result<Void> deactivate() {
        userService.deactivate(SecurityUtil.getUserId());
        return Result.success("账号已注销", null);
    }
}
