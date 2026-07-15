package com.medcare.hda.agent.doctor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.agent.api.DoctorRecommendation;
import com.medcare.hda.agent.api.DoctorRecommendationAction;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.service.DoctorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 医生推荐业务 Tool。只负责受控查询、确定性评分和结构化返回，
 * 不做诊断、不调用模型，也不决定自己何时运行。
 */
@Component
public class DoctorRecommendationTool {
    private static final Map<String, List<String>> DEPARTMENT_TERMS = departmentTerms();
    private final DoctorService doctorService;
    private final int limit;

    public DoctorRecommendationTool(DoctorService doctorService,
                                    @Value("${hda.agent.doctor-recommendation.limit:3}") int limit) {
        this.doctorService = doctorService;
        this.limit = Math.max(1, Math.min(3, limit));
    }

    @Tool(name = "recommend_doctors", description = "根据完成安全分诊和问诊后的健康诉求，从已审核医生库返回最多3位匹配医生")
    public List<DoctorRecommendation> recommend(
            @ToolParam(description = "用户本轮原始消息") String userMessage,
            @ToolParam(description = "逐步问诊完成后的临床摘要") String clinicalSummary) {
        List<Doctor> candidates = doctorService.list(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getStatus, 1)
                .eq(Doctor::getAuditStatus, "APPROVED")
                .isNotNull(Doctor::getUserId));
        DoctorRecommendationCriteria criteria = criteria(userMessage, clinicalSummary, candidates);
        if (criteria.targetDepartments().isEmpty() && criteria.specialtyKeywords().isEmpty()) return List.of();

        return candidates.stream()
                .map(doctor -> score(doctor, criteria))
                .filter(ScoredDoctor::eligible)
                .sorted(Comparator.comparingInt(ScoredDoctor::score).reversed()
                        .thenComparing(Comparator.comparingDouble(ScoredDoctor::departmentFactor).reversed())
                        .thenComparing(Comparator.comparingInt(ScoredDoctor::specialtyMatches).reversed())
                        .thenComparing(sd -> sd.doctor().getId(), Comparator.nullsLast(Long::compareTo)))
                .limit(limit)
                .map(this::toRecommendation)
                .toList();
    }

    private DoctorRecommendationCriteria criteria(String userMessage, String clinicalSummary,
                                                  List<Doctor> candidates) {
        String text = safe(userMessage) + "\n" + safe(clinicalSummary);
        Set<String> departments = new LinkedHashSet<>();
        Set<String> keywords = new LinkedHashSet<>();
        DEPARTMENT_TERMS.forEach((department, terms) -> {
            List<String> matched = terms.stream().filter(text::contains).toList();
            if (!matched.isEmpty() && departments.size() < 3) {
                departments.add(department);
                matched.stream().filter(term -> term.length() >= 2).limit(4).forEach(keywords::add);
            }
        });
        String preferredHospital = candidates.stream().map(Doctor::getHospital)
                .filter(StringUtils::hasText).filter(text::contains).findFirst().orElse("");
        return new DoctorRecommendationCriteria(List.copyOf(departments),
                keywords.stream().limit(8).toList(), preferredHospital);
    }

    private ScoredDoctor score(Doctor doctor, DoctorRecommendationCriteria criteria) {
        String department = normalize(doctor.getDepartment());
        double departmentFactor = 0;
        for (int i = 0; i < criteria.targetDepartments().size(); i++) {
            String target = normalize(criteria.targetDepartments().get(i));
            if (!target.isEmpty() && (department.equals(target) || department.contains(target) || target.contains(department))) {
                departmentFactor = Math.max(departmentFactor, i == 0 ? 1.0 : 0.8);
            }
        }

        String searchable = normalize(String.join(" ", safe(doctor.getSpeciality()), safe(doctor.getIntroduction())));
        Set<String> matchedKeywords = new LinkedHashSet<>();
        for (String keyword : criteria.specialtyKeywords()) {
            String normalized = normalize(keyword);
            if (normalized.length() >= 2 && searchable.contains(normalized)) matchedKeywords.add(keyword.trim());
        }
        int keywordBase = Math.max(1, Math.min(3, criteria.specialtyKeywords().size()));
        double specialtyFactor = Math.min(1.0, matchedKeywords.size() / (double) keywordBase);
        double qualificationFactor = qualificationFactor(doctor.getTitle());
        boolean hospitalRequested = StringUtils.hasText(criteria.preferredHospital());
        double hospitalFactor = hospitalRequested
                && normalize(doctor.getHospital()).contains(normalize(criteria.preferredHospital())) ? 1.0 : 0.0;

        double denominator = hospitalRequested ? 100.0 : 95.0;
        double weighted = departmentFactor * 45 + specialtyFactor * 40 + qualificationFactor * 10
                + hospitalFactor * 5;
        int score = (int) Math.round(weighted / denominator * 100);

        List<String> reasons = new ArrayList<>();
        if (departmentFactor > 0) reasons.add("科室与当前健康诉求匹配");
        if (!matchedKeywords.isEmpty()) reasons.add("擅长领域命中：" + String.join("、", matchedKeywords));
        if (hospitalFactor > 0) reasons.add("符合指定医院偏好");
        if (StringUtils.hasText(doctor.getTitle())) reasons.add("职称：" + doctor.getTitle());
        return new ScoredDoctor(doctor, score, departmentFactor, matchedKeywords.size(),
                departmentFactor > 0 || !matchedKeywords.isEmpty(), List.copyOf(reasons.subList(0, Math.min(3, reasons.size()))));
    }

    private DoctorRecommendation toRecommendation(ScoredDoctor scored) {
        Doctor doctor = scored.doctor();
        return new DoctorRecommendation(doctor.getId(), doctor.getName(), doctor.getAvatar(), doctor.getTitle(),
                doctor.getHospital(), doctor.getDepartment(), doctor.getSpeciality(), doctor.getIntroduction(),
                scored.score(), scored.reasons(),
                new DoctorRecommendationAction("START_DOCTOR_CONSULT", "/doctor-consult", doctor.getId()));
    }

    private double qualificationFactor(String title) {
        if (!StringUtils.hasText(title)) return 0.4;
        if (title.contains("主任医师") && !title.contains("副主任")) return 1.0;
        if (title.contains("副主任医师")) return 0.85;
        if (title.contains("主治医师")) return 0.7;
        return 0.5;
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[\\s、，,;；/]+", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, List<String>> departmentTerms() {
        Map<String, List<String>> terms = new LinkedHashMap<>();
        terms.put("心血管内科", List.of("高血压", "冠心病", "心衰", "心力衰竭", "心律失常", "房颤", "心慌", "胸闷"));
        terms.put("内分泌科", List.of("糖尿病", "血糖", "甲状腺", "肥胖", "骨质疏松"));
        terms.put("神经内科", List.of("脑卒中", "中风", "帕金森", "头痛", "眩晕", "失眠", "睡眠障碍", "认知障碍"));
        terms.put("呼吸内科", List.of("咳嗽", "哮喘", "慢阻肺", "肺部感染", "睡眠呼吸暂停"));
        terms.put("消化内科", List.of("胃炎", "胃痛", "胃溃疡", "便秘", "腹泻", "脂肪肝", "肝硬化", "胆石症"));
        terms.put("骨科", List.of("骨折", "关节炎", "腰椎间盘", "颈肩腰腿痛", "关节痛"));
        terms.put("康复医学科", List.of("康复", "偏瘫", "术后康复", "慢性疼痛"));
        terms.put("老年医学科", List.of("老年", "多重用药", "跌倒", "营养", "慢病管理", "阿尔茨海默"));
        terms.put("中医科", List.of("中医", "针灸", "体质调理", "面瘫"));
        terms.put("眼科", List.of("白内障", "青光眼", "黄斑变性", "视力"));
        terms.put("肿瘤科", List.of("肿瘤", "癌症", "肺癌"));
        terms.put("泌尿外科", List.of("前列腺", "泌尿系结石", "尿失禁"));
        return java.util.Collections.unmodifiableMap(terms);
    }

    private record ScoredDoctor(Doctor doctor, int score, double departmentFactor,
                                int specialtyMatches, boolean eligible, List<String> reasons) {
    }
}
