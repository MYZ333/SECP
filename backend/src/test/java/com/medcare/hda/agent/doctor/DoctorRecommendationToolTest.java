package com.medcare.hda.agent.doctor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.medcare.hda.agent.api.DoctorRecommendation;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.service.DoctorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorRecommendationToolTest {

    @Test
    void shouldReturnAtMostThreeEligibleDoctorsOrderedByScore() {
        DoctorService service = mock(DoctorService.class);
        when(service.list(org.mockito.ArgumentMatchers.<Wrapper<Doctor>>any())).thenReturn(List.of(
                doctor(1L, "李医生", "主任医师", "心血管内科", "高血压、冠心病"),
                doctor(2L, "陈医生", "副主任医师", "心血管内科", "心律失常、房颤"),
                doctor(3L, "王医生", "主任医师", "内分泌科", "糖尿病、甲状腺疾病"),
                doctor(4L, "赵医生", "主治医师", "心血管内科", "高血压长期管理")));

        DoctorRecommendationTool tool = new DoctorRecommendationTool(service, 3);
        List<DoctorRecommendation> result = tool.recommend("血压最近一直偏高，请推荐医生", "反复高血压，需要进一步评估");

        assertEquals(3, result.size());
        assertEquals(1L, result.getFirst().doctorId());
        assertTrue(result.get(0).matchScore() >= result.get(1).matchScore());
        assertTrue(result.get(1).matchScore() >= result.get(2).matchScore());
        assertTrue(result.stream().noneMatch(item -> item.doctorId().equals(3L)));
        assertEquals("START_DOCTOR_CONSULT", result.getFirst().action().type());
    }

    @Test
    void shouldReturnEmptyWhenNoDepartmentOrSpecialtyCanBeInferred() {
        DoctorService service = mock(DoctorService.class);
        when(service.list(org.mockito.ArgumentMatchers.<Wrapper<Doctor>>any())).thenReturn(List.of(
                doctor(1L, "李医生", "主任医师", "心血管内科", "高血压、冠心病")));

        DoctorRecommendationTool tool = new DoctorRecommendationTool(service, 3);

        assertTrue(tool.recommend("请推荐一位医生", "没有提供任何健康方向").isEmpty());
    }

    private Doctor doctor(Long id, String name, String title, String department, String speciality) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setUserId(id + 100);
        doctor.setName(name);
        doctor.setTitle(title);
        doctor.setHospital("市第一人民医院");
        doctor.setDepartment(department);
        doctor.setSpeciality(speciality);
        doctor.setIntroduction(speciality + "相关诊疗");
        doctor.setStatus(1);
        doctor.setAuditStatus("APPROVED");
        return doctor;
    }
}
