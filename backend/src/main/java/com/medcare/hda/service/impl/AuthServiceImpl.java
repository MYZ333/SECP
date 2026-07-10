package com.medcare.hda.service.impl;

import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.JwtProperties;
import com.medcare.hda.security.JwtUtil;
import com.medcare.hda.service.AuthService;
import com.medcare.hda.service.PointService;
import com.medcare.hda.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_KEY_PREFIX = "hda:login:token:";

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        // 将 token 存入 Redis（登录态），过期时间与 JWT 一致
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + user.getId(), token,
                jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);
        // 登录积分奖励（每日首次可在此扩展；此处示例每次登录 +1）
        pointService.addPoints(user.getId(), 1, "LOGIN", "登录奖励");
        return LoginVO.builder()
                .token(token)
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
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole("USER");
        user.setPoints(0);
        user.setStatus(0);
        user.setGender(0);
        userService.save(user);
    }
}
