package com.medcare.hda.dto;

import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "医生咨询会话视图")
public class DoctorConsultSessionVO {
    private DoctorConsultSession session;
    private Doctor doctor;
    private User patient;
}
