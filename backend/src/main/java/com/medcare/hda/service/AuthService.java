package com.medcare.hda.service;

import com.medcare.hda.dto.LoginDTO;
import com.medcare.hda.dto.LoginVO;
import com.medcare.hda.dto.RegisterDTO;

public interface AuthService {
    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);
}
