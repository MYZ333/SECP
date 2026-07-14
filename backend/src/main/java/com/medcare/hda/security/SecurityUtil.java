package com.medcare.hda.security;

import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 获取当前登录用户的工具 */
public class SecurityUtil {

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static boolean isAdmin() {
        return getLoginUser().hasRole("ADMIN");
    }

    public static boolean hasRole(String role) {
        return getLoginUser().hasRole(role);
    }
}
