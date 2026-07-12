package com.medcare.hda.config;

import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.entity.MedicalRecord;
import com.medcare.hda.mapper.HealthProfileMapper;
import com.medcare.hda.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 存量数据加密迁移：把敏感字段的遗留明文加密回填。
 * 幂等：读取时已加密的会先解密、写入时再加密，重复跑不会二次加密。
 * 仅当 hda.crypto.migrate=true 时执行；迁移完成后请改回 false。
 * 上线前务必先备份数据库！
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hda.crypto.migrate", havingValue = "true")
public class DataEncryptMigrationRunner implements ApplicationRunner {

    private final MedicalRecordMapper medicalRecordMapper;
    private final HealthProfileMapper healthProfileMapper;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("==== 敏感字段加密迁移开始（请确保已备份数据库）====");
        int mr = migrateMedicalRecords();
        int hp = migrateHealthProfiles();
        log.warn("==== 加密迁移完成：medical_record {} 条，health_profile {} 条。请将 hda.crypto.migrate 改回 false ====",
                mr, hp);
    }

    private int migrateMedicalRecords() {
        List<MedicalRecord> list = medicalRecordMapper.selectList(null);
        for (MedicalRecord r : list) {
            // 读取时字段已解密为明文，updateById 时 TypeHandler 会再加密
            medicalRecordMapper.updateById(r);
        }
        return list.size();
    }

    private int migrateHealthProfiles() {
        List<HealthProfile> list = healthProfileMapper.selectList(null);
        for (HealthProfile p : list) {
            healthProfileMapper.updateById(p);
        }
        return list.size();
    }
}
