package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.ChangePasswordDTO;
import com.medcare.hda.dto.UpdateProfileDTO;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.mapper.UserMapper;
import com.medcare.hda.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public User getByUsername(String username) {
        return getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    @Override
    public User updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getBirthday() != null) user.setBirthday(dto.getBirthday());
        updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
    }

    @Override
    public void deactivate(Long userId) {
        removeById(userId);
    }
}
