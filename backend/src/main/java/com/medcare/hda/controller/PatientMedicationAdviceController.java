package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.entity.MedicationAdvice;
import com.medcare.hda.entity.MedicationAdviceItem;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.DoctorConsultSessionService;
import com.medcare.hda.service.MedicationAdviceItemService;
import com.medcare.hda.service.MedicationAdviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "患者端-用药建议")
@RestController
@RequestMapping("/api/doctor-consult")
@RequiredArgsConstructor
public class PatientMedicationAdviceController {

    private final DoctorConsultSessionService sessionService;
    private final MedicationAdviceService adviceService;
    private final MedicationAdviceItemService itemService;

    @Operation(summary = "查看会话用药建议")
    @GetMapping("/session/{sessionId}/medication-advice")
    public Result<MedicationAdvice> getAdvice(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkPatientSession(sessionId);
        return Result.success(loadAdvice(session.getId()));
    }

    @Operation(summary = "患者确认用药建议")
    @PutMapping("/medication-advice/{adviceId}/confirm")
    public Result<MedicationAdvice> confirm(@PathVariable Long adviceId) {
        MedicationAdvice advice = adviceService.getById(adviceId);
        if (advice == null || !advice.getUserId().equals(SecurityUtil.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        advice.setStatus("CONFIRMED");
        advice.setPatientConfirmTime(LocalDateTime.now());
        adviceService.updateById(advice);
        return Result.success("已确认用药建议", loadAdvice(advice.getSessionId()));
    }

    private DoctorConsultSession checkPatientSession(Long sessionId) {
        DoctorConsultSession session = sessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(SecurityUtil.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return session;
    }

    private MedicationAdvice loadAdvice(Long sessionId) {
        MedicationAdvice advice = adviceService.getOne(Wrappers.<MedicationAdvice>lambdaQuery()
                .eq(MedicationAdvice::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (advice != null) {
            advice.setItems(itemService.list(Wrappers.<MedicationAdviceItem>lambdaQuery()
                    .eq(MedicationAdviceItem::getAdviceId, advice.getId())
                    .orderByAsc(MedicationAdviceItem::getSortOrder)
                    .orderByAsc(MedicationAdviceItem::getId)));
        }
        return advice;
    }
}
