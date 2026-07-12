package com.medcare.hda.dto;

import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.entity.HealthReport;
import com.medcare.hda.entity.MedicalRecord;
import com.medcare.hda.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "医生端患者详情")
public class PatientDetailVO {
    private User patient;
    private Integer age;
    private HealthProfile profile;
    private List<HealthMetric> metrics;
    private List<MedicalRecord> medicalRecords;
    private List<HealthReport> reports;
    private List<HealthAlert> alerts;
}
