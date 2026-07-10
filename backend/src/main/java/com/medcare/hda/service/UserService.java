package com.medcare.hda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medcare.hda.dto.ChangePasswordDTO;
import com.medcare.hda.dto.UpdateProfileDTO;
import com.medcare.hda.entity.User;

public interface UserService extends IService<User> {

    User getByUsername(String username);

    /** 更新当前用户资料 */
    User updateProfile(Long userId, UpdateProfileDTO dto);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 注销账号(逻辑删除) */
    void deactivate(Long userId);
}
