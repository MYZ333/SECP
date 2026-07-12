package com.medcare.hda.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.PhoneLoginDTO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.dto.ResetPasswordDTO;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.JwtProperties;
import com.medcare.hda.security.JwtUtil;
import com.medcare.hda.security.RsaCryptoService;
import com.medcare.hda.service.AuthService;
import com.medcare.hda.service.PointService;
import com.medcare.hda.service.SmsService;
import com.medcare.hda.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final RsaCryptoService rsaCryptoService;

    private static final String TOKEN_KEY_PREFIX = "hda:login:token:";       // 单点登录态：当前有效 access token
    private static final String REFRESH_KEY_PREFIX = "hda:login:refresh:";   // 当前有效 refresh token
    private static final String BLACKLIST_PREFIX = "hda:token:blacklist:";   // 已登出/失效的 jti

    @Override
    public LoginVO login(LoginDTO dto) {
        String rawPassword = rsaCryptoService.resolvePassword(dto.getPassword());
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        return issueToken(user);
    }

    @Override
    public LoginVO phoneLogin(PhoneLoginDTO dto) {
        smsService.verifyCode(dto.getPhone(), dto.getCode());
        User user = findEarliestByPhone(dto.getPhone());
        if (user == null) {
            user = autoRegister(dto.getPhone());
        }
        return issueToken(user);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        smsService.verifyCode(dto.getPhone(), dto.getCode());
        User user = findEarliestByPhone(dto.getPhone());
        if (user == null) {
            throw new BusinessException(ResultCode.PHONE_NOT_REGISTERED);
        }
        String rawPassword = validatedPassword(rsaCryptoService.resolvePassword(dto.getNewPassword()));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userService.updateById(user);
        // 使旧登录态失效，需重新登录
        redisTemplate.delete(TOKEN_KEY_PREFIX + user.getId());
        redisTemplate.delete(REFRESH_KEY_PREFIX + user.getId());
        log.info("用户 {}(id={}) 通过手机号重置了密码", user.getUsername(), user.getId());
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims = jwtUtil.parseToken(refreshToken);
        if (claims == null || !jwtUtil.isRefreshToken(claims)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = claims.get("userId", Long.class);
        // 校验是否为该用户当前有效的 refresh token（改密/登出/新登录后即失效）
        Object saved = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (saved == null || !saved.equals(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        // 轮换：重新签发 access + refresh，防止 refresh token 重放
        return issueToken(user);
    }

    @Override
    public void logout(String accessToken) {
        Claims claims = jwtUtil.parseToken(accessToken);
        if (claims == null) {
            return;
        }
        Long userId = claims.get("userId", Long.class);
        String jti = claims.getId();
        long remaining = jwtUtil.getRemainingMillis(claims);
        if (jti != null && remaining > 0) {
            // 加入黑名单，TTL = token 剩余有效期，过期自动清理
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", remaining, TimeUnit.MILLISECONDS);
        }
        if (userId != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        }
        log.info("用户 id={} 已登出", userId);
    }

    /** 按手机号查用户；历史数据可能存在同号多账号，取最早注册的一个（新库已加唯一索引） */
    private User findEarliestByPhone(String phone) {
        return userService.lambdaQuery()
                .eq(User::getPhone, phone)
                .orderByAsc(User::getId)
                .last("LIMIT 1")
                .one();
    }

    /** 手机号未注册时自动注册：随机用户名 + 随机密码（可在账户管理中修改），昵称=用户+尾号4位 */
    private User autoRegister(String phone) {
        String username;
        do {
            username = "user_" + RandomUtil.randomString("abcdefghjkmnpqrstuvwxyz23456789", 8);
        } while (userService.getByUsername(username) != null);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(RandomUtil.randomString(20)));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setPhone(phone);
        user.setRole("USER");
        user.setPoints(0);
        user.setStatus(0);
        user.setGender(0);
        userService.save(user);
        log.info("手机号 {} 自动注册新账号: {}", phone, username);
        return user;
    }

    /** 校验账号状态、签发双令牌、写 Redis 登录态、发登录积分 —— 账密/手机号登录/刷新共用 */
    private LoginVO issueToken(User user) {
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        // 单点登录：每个用户只保留一个有效 access / refresh，新登录覆盖旧的 → 旧端自动被挤下线
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + user.getId(), accessToken,
                jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + user.getId(), refreshToken,
                jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);
        // 登录任务：标记为"待领取"，由用户到积分中心手动领取（不再自动发放）
        pointService.markTaskReady(user.getId(), "LOGIN");
        return LoginVO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpiration())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void register(RegisterDTO dto) {
        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        String rawPassword = validatedPassword(rsaCryptoService.resolvePassword(dto.getPassword()));
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole("USER");
        user.setPoints(0);
        user.setStatus(0);
        user.setGender(0);
        userService.save(user);
    }

    /** RSA 解密后再校验密码长度（因为密文形态下 DTO 的长度校验放宽了） */
    private String validatedPassword(String raw) {
        if (raw == null || raw.length() < 6 || raw.length() > 32) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码长度为 6-32 位");
        }
        return raw;
    }
}
