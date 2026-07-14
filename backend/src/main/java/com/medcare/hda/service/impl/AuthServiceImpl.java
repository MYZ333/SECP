package com.medcare.hda.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.PhoneLoginDTO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.dto.ResetPasswordDTO;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.JwtProperties;
import com.medcare.hda.security.JwtUtil;
import com.medcare.hda.security.RsaCryptoService;
import com.medcare.hda.service.AuthService;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.PointService;
import com.medcare.hda.service.SmsService;
import com.medcare.hda.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final DoctorService doctorService;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final RsaCryptoService rsaCryptoService;

    private static final String TOKEN_KEY_PREFIX = "hda:login:token:";
    private static final String REFRESH_KEY_PREFIX = "hda:login:refresh:";
    private static final String BLACKLIST_PREFIX = "hda:token:blacklist:";

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = authenticate(dto);
        List<String> roles = userService.listRoleCodes(user.getId());
        if (!roles.contains("PATIENT") && !roles.contains("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "This account cannot log in from the patient web");
        }
        String activeRole = roles.contains("ADMIN") ? "ADMIN" : "PATIENT";
        return issueToken(user, activeRole);
    }

    @Override
    public LoginVO doctorLogin(LoginDTO dto) {
        User user = authenticate(dto);
        if (!userService.hasRole(user.getId(), "DOCTOR")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "This account is not a doctor account");
        }
        assertDoctorCanLogin(user.getId());
        return issueToken(user, "DOCTOR");
    }

    private User authenticate(LoginDTO dto) {
        String rawPassword = rsaCryptoService.resolvePassword(dto.getPassword());
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        return user;
    }

    @Override
    public LoginVO phoneLogin(PhoneLoginDTO dto) {
        smsService.verifyCode(dto.getPhone(), dto.getCode());
        User user = findEarliestByPhone(dto.getPhone());
        if (user == null) {
            user = autoRegister(dto.getPhone());
        }
        if (!userService.hasRole(user.getId(), "PATIENT")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "This phone number is not bound to a patient account");
        }
        return issueToken(user, "PATIENT");
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
        redisTemplate.delete(TOKEN_KEY_PREFIX + user.getId());
        redisTemplate.delete(REFRESH_KEY_PREFIX + user.getId());
        log.info("User {}(id={}) reset password by phone", user.getUsername(), user.getId());
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims = jwtUtil.parseToken(refreshToken);
        if (claims == null || !jwtUtil.isRefreshToken(claims)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = claims.get("userId", Long.class);
        Object saved = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (saved == null || !saved.equals(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        String activeRole = claims.get("role", String.class);
        if (activeRole == null || !userService.hasRole(userId, activeRole)) {
            activeRole = defaultRole(userService.listRoleCodes(userId));
        }
        if ("DOCTOR".equals(activeRole)) {
            assertDoctorCanLogin(userId);
        }
        return issueToken(user, activeRole);
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
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", remaining, TimeUnit.MILLISECONDS);
        }
        if (userId != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        }
        log.info("User id={} logged out", userId);
    }

    private User findEarliestByPhone(String phone) {
        return userService.lambdaQuery()
                .eq(User::getPhone, phone)
                .orderByAsc(User::getId)
                .last("LIMIT 1")
                .one();
    }

    private User autoRegister(String phone) {
        String username;
        do {
            username = "user_" + RandomUtil.randomString("abcdefghjkmnpqrstuvwxyz23456789", 8);
        } while (userService.getByUsername(username) != null);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(RandomUtil.randomString(20)));
        user.setNickname("User" + phone.substring(phone.length() - 4));
        user.setPhone(phone);
        user.setStatus(0);
        userService.save(user);
        userService.assignRole(user.getId(), "PATIENT");
        userService.ensurePatientResources(user.getId());
        log.info("Phone {} auto registered patient account {}", phone, username);
        return userService.getById(user.getId());
    }

    private LoginVO issueToken(User user, String activeRole) {
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        if (activeRole == null || !userService.hasRole(user.getId(), activeRole)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), activeRole);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), activeRole);
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + user.getId(), accessToken,
                jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + user.getId(), refreshToken,
                jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);
        if (userService.hasRole(user.getId(), "PATIENT")) {
            pointService.markTaskReady(user.getId(), "LOGIN");
        }
        User snapshot = userService.populateUserSnapshot(user);
        return LoginVO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpiration())
                .userId(snapshot.getId())
                .username(snapshot.getUsername())
                .nickname(snapshot.getNickname())
                .role(activeRole)
                .roles(userService.listRoleCodes(snapshot.getId()))
                .avatar(snapshot.getAvatar())
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
        user.setStatus(0);
        userService.save(user);
        userService.assignRole(user.getId(), "PATIENT");
        userService.ensurePatientResources(user.getId());
    }

    private void assertDoctorCanLogin(Long userId) {
        Doctor doctor = doctorService.getOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, userId)
                .last("LIMIT 1"));
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1
                || !"APPROVED".equals(doctor.getAuditStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Doctor account is not approved or enabled");
        }
    }

    private String defaultRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("PATIENT")) return "PATIENT";
        if (roles.contains("DOCTOR")) return "DOCTOR";
        return null;
    }

    private String validatedPassword(String raw) {
        if (raw == null || raw.length() < 6 || raw.length() > 32) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Password length must be 6-32 characters");
        }
        return raw;
    }
}
