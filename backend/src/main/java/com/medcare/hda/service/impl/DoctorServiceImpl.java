package com.medcare.hda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.mapper.DoctorConsultSessionMapper;
import com.medcare.hda.mapper.DoctorMapper;
import com.medcare.hda.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    private final DoctorConsultSessionMapper sessionMapper;

    @Override
    @Cacheable(value = "doctor:detail", key = "#id", unless = "#result == null")
    public Doctor getCachedById(Long id) {
        return getById(id);
    }

    @Override
    public List<String> listActiveDepartments() {
        // 科室列表随医生数据实时变化（且常通过 SQL 直接维护），不缓存，避免筛选 tab 显示过期
        return listObjs(
                Wrappers.<Doctor>query()
                        .select("DISTINCT department")
                        .eq("status", 1)
                        .isNotNull("department")
                        .orderByAsc("department"),
                Objects::toString);
    }

    @Override
    public void populateRatingStats(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return;
        }
        populateRatingStats(List.of(doctor));
    }

    @Override
    public void populateRatingStats(List<Doctor> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return;
        }
        doctors.forEach(this::resetRatingStats);
        List<Long> doctorIds = doctors.stream()
                .map(Doctor::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (doctorIds.isEmpty()) {
            return;
        }

        List<Map<String, Object>> rows = sessionMapper.selectMaps(new QueryWrapper<DoctorConsultSession>()
                .select("doctor_id", "rating", "COUNT(*) AS rating_count")
                .in("doctor_id", doctorIds)
                .isNotNull("rating")
                .groupBy("doctor_id", "rating"));

        Map<Long, RatingAccumulator> statsByDoctor = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long doctorId = toLong(row.get("doctor_id"));
            Integer rating = toInteger(row.get("rating"));
            Integer count = toInteger(row.get("rating_count"));
            if (doctorId == null || rating == null || rating < 1 || rating > 5 || count == null || count <= 0) {
                continue;
            }
            statsByDoctor.computeIfAbsent(doctorId, id -> new RatingAccumulator()).add(rating, count);
        }

        Map<Long, Doctor> doctorById = doctors.stream()
                .filter(doctor -> doctor.getId() != null)
                .collect(Collectors.toMap(Doctor::getId, doctor -> doctor, (left, right) -> left));
        statsByDoctor.forEach((doctorId, stats) -> {
            Doctor doctor = doctorById.get(doctorId);
            if (doctor == null) {
                return;
            }
            doctor.setRatingCounts(stats.counts);
            doctor.setRatingCount(stats.total);
            doctor.setAverageRating(stats.average());
        });
    }

    private void resetRatingStats(Doctor doctor) {
        if (doctor == null) return;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            counts.put(rating, 0);
        }
        doctor.setRatingCounts(counts);
        doctor.setRatingCount(0);
        doctor.setAverageRating(0.0);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        return Long.parseLong(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return null;
        return Integer.parseInt(String.valueOf(value));
    }

    private static class RatingAccumulator {
        private final Map<Integer, Integer> counts = new HashMap<>();
        private int total;
        private int scoreSum;

        private RatingAccumulator() {
            for (int rating = 1; rating <= 5; rating++) {
                counts.put(rating, 0);
            }
        }

        private void add(int rating, int count) {
            counts.put(rating, count);
            total += count;
            scoreSum += rating * count;
        }

        private double average() {
            if (total == 0) return 0.0;
            return BigDecimal.valueOf((double) scoreSum / total)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    /** 医生被修改/删除时，清空详情缓存，保证后台改动立即生效 */
    @Override
    @CacheEvict(value = "doctor:detail", allEntries = true)
    public boolean save(Doctor entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "doctor:detail", allEntries = true)
    public boolean updateById(Doctor entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "doctor:detail", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
