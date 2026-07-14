package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.MedicationAdviceDTO;
import com.medcare.hda.dto.MedicationAdviceItemDTO;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.entity.Medicine;
import com.medcare.hda.entity.MedicationAdvice;
import com.medcare.hda.entity.MedicationAdviceItem;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.DoctorConsultSessionService;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.MedicineService;
import com.medcare.hda.service.MedicationAdviceItemService;
import com.medcare.hda.service.MedicationAdviceService;
import com.medcare.hda.websocket.DoctorConsultNotifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "医生端-用药建议")
@RestController
@RequestMapping("/api/doctor-portal")
@RequiredArgsConstructor
public class DoctorMedicationAdviceController {

    private final DoctorService doctorService;
    private final DoctorConsultSessionService sessionService;
    private final MedicineService medicineService;
    private final MedicationAdviceService adviceService;
    private final MedicationAdviceItemService itemService;
    private final DoctorConsultNotifier notifier;

    @Operation(summary = "搜索启用药品")
    @GetMapping("/medicines")
    public Result<List<Medicine>> medicines(@RequestParam(required = false) String keyword) {
        currentDoctor();
        var query = Wrappers.<Medicine>lambdaQuery()
                .eq(Medicine::getStatus, 1)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Medicine::getName, keyword)
                        .or().like(Medicine::getGenericName, keyword)
                        .or().like(Medicine::getBrandName, keyword)
                        .or().like(Medicine::getCategory, keyword))
                .orderByAsc(Medicine::getCategory)
                .orderByAsc(Medicine::getName)
                .last("LIMIT 50");
        return Result.success(medicineService.list(query));
    }

    @Operation(summary = "查看会话用药建议")
    @GetMapping("/session/{sessionId}/medication-advice")
    public Result<MedicationAdvice> getAdvice(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkDoctorSession(sessionId);
        return Result.success(loadAdvice(session.getId()));
    }

    @Operation(summary = "保存会话用药建议")
    @PostMapping("/session/{sessionId}/medication-advice")
    @Transactional(rollbackFor = Exception.class)
    public Result<MedicationAdvice> saveAdvice(@PathVariable Long sessionId,
                                               @Valid @RequestBody MedicationAdviceDTO dto) {
        DoctorConsultSession session = checkDoctorSession(sessionId);
        if (!"CLOSED".equals(session.getStatus()) || !hasDoctorReport(session)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "医生提交咨询报告后才能添加用药建议");
        }
        if (Integer.valueOf(1).equals(session.getRecommendOffline())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已建议患者线下就医，不建议线上添加用药建议");
        }

        MedicationAdvice advice = adviceService.getOne(Wrappers.<MedicationAdvice>lambdaQuery()
                .eq(MedicationAdvice::getSessionId, session.getId())
                .last("LIMIT 1"));
        if (advice != null && "CONFIRMED".equals(advice.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "患者已确认的用药建议不能修改");
        }
        if (advice == null) {
            advice = new MedicationAdvice();
            advice.setSessionId(session.getId());
            advice.setDoctorId(session.getDoctorId());
            advice.setUserId(session.getUserId());
            advice.setStatus("PENDING_CONFIRM");
            advice.setDoctorNote(cleanText(dto.getDoctorNote()));
            adviceService.save(advice);
        } else {
            advice.setStatus("PENDING_CONFIRM");
            advice.setDoctorNote(cleanText(dto.getDoctorNote()));
            adviceService.updateById(advice);
            itemService.remove(Wrappers.<MedicationAdviceItem>lambdaQuery()
                    .eq(MedicationAdviceItem::getAdviceId, advice.getId()));
        }

        List<MedicationAdviceItem> items = buildItems(advice.getId(), dto.getItems());
        itemService.saveBatch(items);
        advice.setItems(items);
        notifier.notifyUser(session.getUserId(), "DOCTOR_CONSULT_MEDICATION_ADVICE", advice);
        return Result.success("用药建议已发送给患者", advice);
    }

    private List<MedicationAdviceItem> buildItems(Long adviceId, List<MedicationAdviceItemDTO> dtoItems) {
        List<MedicationAdviceItem> items = new ArrayList<>();
        for (int i = 0; i < dtoItems.size(); i++) {
            MedicationAdviceItemDTO dto = dtoItems.get(i);
            Medicine medicine = medicineService.getById(dto.getMedicineId());
            if (medicine == null || !Integer.valueOf(1).equals(medicine.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "药品不存在或已停用");
            }
            if (Integer.valueOf(1).equals(medicine.getRequiresOffline())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "“" + medicine.getName() + "”需要线下就医或处方资质确认");
            }
            Integer durationDays = dto.getDurationDays();
            if (durationDays == null || durationDays <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请填写“" + medicine.getName() + "”的用药天数");
            }
            if (medicine.getMaxDurationDays() != null && medicine.getMaxDurationDays() > 0 && durationDays > medicine.getMaxDurationDays()) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "“" + medicine.getName() + "”用药天数不能超过" + medicine.getMaxDurationDays() + "天");
            }
            if (!StringUtils.hasText(dto.getDosage()) || !StringUtils.hasText(dto.getFrequency())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请填写“" + medicine.getName() + "”的剂量和频次");
            }
            MedicationAdviceItem item = new MedicationAdviceItem();
            item.setAdviceId(adviceId);
            item.setMedicineId(medicine.getId());
            item.setMedicineName(medicine.getName());
            item.setSpecification(medicine.getSpecification());
            item.setUsageMethod(cleanText(dto.getUsageMethod()));
            item.setDosage(cleanText(dto.getDosage()));
            item.setFrequency(cleanText(dto.getFrequency()));
            item.setDurationDays(durationDays);
            item.setQuantity(cleanText(dto.getQuantity()));
            item.setPrecautions(StringUtils.hasText(dto.getPrecautions()) ? dto.getPrecautions().trim() : medicine.getPrecautions());
            item.setSortOrder(i + 1);
            items.add(item);
        }
        return items;
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

    private DoctorConsultSession checkDoctorSession(Long sessionId) {
        Doctor doctor = currentDoctor();
        DoctorConsultSession session = sessionService.getById(sessionId);
        if (session == null || !session.getDoctorId().equals(doctor.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return session;
    }

    private Doctor currentDoctor() {
        if (!SecurityUtil.hasRole("DOCTOR")) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Doctor doctor = doctorService.getOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, SecurityUtil.getUserId())
                .last("LIMIT 1"));
        if (doctor == null) throw new BusinessException(ResultCode.FORBIDDEN);
        return doctor;
    }

    private boolean hasDoctorReport(DoctorConsultSession session) {
        return StringUtils.hasText(session.getProblemOverview())
                || StringUtils.hasText(session.getPreliminaryAssessment())
                || StringUtils.hasText(session.getSummary())
                || StringUtils.hasText(session.getAdvice())
                || StringUtils.hasText(session.getRiskWarning());
    }

    private String cleanText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
