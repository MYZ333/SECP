package com.medcare.hda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medcare.hda.dto.ChangePasswordDTO;
import com.medcare.hda.dto.UpdateProfileDTO;
import com.medcare.hda.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User getByUsername(String username);

    List<String> listRoleCodes(Long userId);

    boolean hasRole(Long userId, String roleCode);

    void assignRole(Long userId, String roleCode);

    void ensurePatientResources(Long userId);

    User populateUserSnapshot(User user);

    User updateProfile(Long userId, UpdateProfileDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    void deactivate(Long userId);
}
