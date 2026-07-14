package com.medcare.hda.service;

import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.PhoneLoginDTO;
import com.medcare.hda.dto.RegisterDTO;
import com.medcare.hda.dto.ResetPasswordDTO;

public interface AuthService {
    LoginVO login(LoginDTO dto);

    LoginVO doctorLogin(LoginDTO dto);

    void register(RegisterDTO dto);

    LoginVO phoneLogin(PhoneLoginDTO dto);

    void resetPassword(ResetPasswordDTO dto);

    LoginVO refresh(String refreshToken);

    void logout(String accessToken);
}
