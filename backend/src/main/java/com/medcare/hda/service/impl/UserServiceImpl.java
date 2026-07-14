package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.ChangePasswordDTO;
import com.medcare.hda.dto.UpdateProfileDTO;
import com.medcare.hda.entity.PatientProfile;
import com.medcare.hda.entity.PointAccount;
import com.medcare.hda.entity.Role;
import com.medcare.hda.entity.User;
import com.medcare.hda.entity.UserRole;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.mapper.PatientProfileMapper;
import com.medcare.hda.mapper.PointAccountMapper;
import com.medcare.hda.mapper.RoleMapper;
import com.medcare.hda.mapper.UserMapper;
import com.medcare.hda.mapper.UserRoleMapper;
import com.medcare.hda.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final PointAccountMapper pointAccountMapper;

    @Override
    public User getByUsername(String username) {
        return populateUserSnapshot(getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
    }

    @Override
    public User getById(Serializable id) {
        return populateUserSnapshot(super.getById(id));
    }

    @Override
    public List<String> listRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return listRoleCodes(userId).contains(roleCode);
    }

    @Override
    public void assignRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Unknown role: " + roleCode);
        }
        Long existing = userRoleMapper.selectCount(Wrappers.<UserRole>lambdaQuery()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, role.getId()));
        if (existing != null && existing > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public void ensurePatientResources(Long userId) {
        PatientProfile profile = patientProfileMapper.selectOne(Wrappers.<PatientProfile>lambdaQuery()
                .eq(PatientProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            profile = new PatientProfile();
            profile.setUserId(userId);
            profile.setGender(0);
            patientProfileMapper.insert(profile);
        }
        PointAccount account = pointAccountMapper.selectOne(Wrappers.<PointAccount>lambdaQuery()
                .eq(PointAccount::getUserId, userId)
                .last("LIMIT 1"));
        if (account == null) {
            account = new PointAccount();
            account.setUserId(userId);
            account.setBalance(0);
            pointAccountMapper.insert(account);
        }
    }

    @Override
    public User populateUserSnapshot(User user) {
        if (user == null) {
            return null;
        }
        List<String> roles = listRoleCodes(user.getId());
        user.setRoles(roles);
        user.setRole(defaultRole(roles));
        if (roles.contains("PATIENT")) {
            PatientProfile profile = patientProfileMapper.selectOne(Wrappers.<PatientProfile>lambdaQuery()
                    .eq(PatientProfile::getUserId, user.getId())
                    .last("LIMIT 1"));
            if (profile != null) {
                user.setGender(profile.getGender());
                user.setBirthday(profile.getBirthday());
            }
            PointAccount account = pointAccountMapper.selectOne(Wrappers.<PointAccount>lambdaQuery()
                    .eq(PointAccount::getUserId, user.getId())
                    .last("LIMIT 1"));
            user.setPoints(account == null || account.getBalance() == null ? 0 : account.getBalance());
        }
        return user;
    }

    private String defaultRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("PATIENT")) return "PATIENT";
        if (roles.contains("DOCTOR")) return "DOCTOR";
        return null;
    }

    @Override
    public User updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = super.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        updateById(user);

        if (hasRole(userId, "PATIENT") && (dto.getGender() != null || dto.getBirthday() != null)) {
            ensurePatientResources(userId);
            PatientProfile profile = patientProfileMapper.selectOne(Wrappers.<PatientProfile>lambdaQuery()
                    .eq(PatientProfile::getUserId, userId)
                    .last("LIMIT 1"));
            if (dto.getGender() != null) profile.setGender(dto.getGender());
            if (dto.getBirthday() != null) profile.setBirthday(dto.getBirthday());
            patientProfileMapper.updateById(profile);
        }
        user.setPassword(null);
        return populateUserSnapshot(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = super.getById(userId);
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
        User user = super.getById(userId);
        if (user == null) {
            return;
        }
        update(Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getUsername, user.getUsername() + "_del_" + userId)
                .set(User::getPhone, null));
        removeById(userId);
    }
}
