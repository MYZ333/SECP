package com.medcare.hda.controller;

import cn.hutool.core.util.StrUtil;
import com.medcare.hda.annotation.RateLimit;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ratelimit.LimitDimension;
import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.PhoneLoginDTO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.dto.ResetPasswordDTO;
import com.medcare.hda.dto.SmsCodeDTO;
import com.medcare.hda.dto.TokenRefreshDTO;
import com.medcare.hda.security.JwtProperties;
import com.medcare.hda.security.RsaCryptoService;
import com.medcare.hda.service.AuthService;
import com.medcare.hda.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证", description = "登录 / 注册 / 短信验证码 / 忘记密码 / 令牌刷新 / 登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsService smsService;
    private final RsaCryptoService rsaCryptoService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "获取 RSA 公钥(用于前端加密密码)")
    @GetMapping("/public-key")
    public Result<Map<String, String>> publicKey() {
        return Result.success(Map.of("publicKey", rsaCryptoService.getPublicKeyBase64()));
    }

    @Operation(summary = "登录")
    @RateLimit(key = "login", window = 60, limit = 5, dimension = LimitDimension.IP,
            message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @Operation(summary = "注册")
    @RateLimit(key = "register", window = 60, limit = 5, dimension = LimitDimension.IP)
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功", null);
    }

    @Operation(summary = "发送短信验证码(登录/忘记密码共用)")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto, HttpServletRequest request) {
        boolean mock = smsService.sendCode(dto.getPhone(), clientIp(request));
        return Result.success(mock ? "验证码已发送（开发模式：请查看后端日志）" : "验证码已发送", null);
    }

    @Operation(summary = "手机号验证码登录(未注册自动注册)")
    @RateLimit(key = "phone-login", window = 60, limit = 5, dimension = LimitDimension.IP)
    @PostMapping("/phone-login")
    public Result<LoginVO> phoneLogin(@Valid @RequestBody PhoneLoginDTO dto) {
        return Result.success("登录成功", authService.phoneLogin(dto));
    }

    @Operation(summary = "忘记密码-重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success("密码已重置，请使用新密码登录", null);
    }

    @Operation(summary = "刷新令牌(用 refreshToken 换取新的 access token)")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody TokenRefreshDTO dto) {
        return Result.success("刷新成功", authService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtProperties.getTokenPrefix())) {
            authService.logout(header.substring(jwtProperties.getTokenPrefix().length()));
        }
        return Result.success("已登出", null);
    }

    /** 取真实客户端 IP（兼容反向代理） */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
