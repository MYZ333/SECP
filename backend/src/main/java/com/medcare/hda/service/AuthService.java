package com.medcare.hda.service;

import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.PhoneLoginDTO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.dto.ResetPasswordDTO;

public interface AuthService {
    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);

    /** 手机号验证码登录；手机号未注册时自动注册并登录 */
    LoginVO phoneLogin(PhoneLoginDTO dto);

    /** 忘记密码：手机号+验证码重置新密码，并使旧登录态失效 */
    void resetPassword(ResetPasswordDTO dto);

    /** 用 refresh token 换取新的 access token（并轮换 refresh token 防重放） */
    LoginVO refresh(String refreshToken);

    /** 登出：当前 access token 加入黑名单，清除登录态与 refresh token */
    void logout(String accessToken);
}
