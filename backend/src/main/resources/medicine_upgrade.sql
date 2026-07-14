-- 药品库与用药建议单升级脚本
-- 已有 hda_db 执行本脚本；全新初始化可直接执行 schema.sql。
USE hda_db;

CREATE TABLE IF NOT EXISTS medicine (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    name                  VARCHAR(100) NOT NULL COMMENT '药品名称',
    generic_name          VARCHAR(100)          DEFAULT NULL COMMENT '通用名',
    brand_name            VARCHAR(100)          DEFAULT NULL COMMENT '商品名',
    category              VARCHAR(50)           DEFAULT NULL COMMENT '药品分类',
    dosage_form           VARCHAR(50)           DEFAULT NULL COMMENT '剂型',
    specification         VARCHAR(100)          DEFAULT NULL COMMENT '规格',
    unit                  VARCHAR(20)           DEFAULT NULL COMMENT '单位',
    default_usage         VARCHAR(100)          DEFAULT NULL COMMENT '常用用法',
    default_dosage        VARCHAR(100)          DEFAULT NULL COMMENT '常用剂量',
    default_frequency     VARCHAR(100)          DEFAULT NULL COMMENT '常用频次',
    default_duration_days INT          NOT NULL DEFAULT 0 COMMENT '建议疗程天数',
    max_duration_days     INT          NOT NULL DEFAULT 0 COMMENT '最大建议天数',
    indications           VARCHAR(1000)         DEFAULT NULL COMMENT '适应症说明',
    contraindications     VARCHAR(1000)         DEFAULT NULL COMMENT '禁忌症',
    precautions           VARCHAR(1000)         DEFAULT NULL COMMENT '注意事项',
    adverse_reactions     VARCHAR(1000)         DEFAULT NULL COMMENT '不良反应',
    requires_offline      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需要线下就医或处方资质',
    status                TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0停用1启用',
    create_time           DATETIME              DEFAULT NULL,
    update_time           DATETIME              DEFAULT NULL,
    deleted               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_medicine_name (name),
    KEY idx_medicine_category (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品库';

CREATE TABLE IF NOT EXISTS medication_advice (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    session_id           BIGINT      NOT NULL COMMENT '咨询会话ID',
    doctor_id            BIGINT      NOT NULL COMMENT '医生ID',
    user_id              BIGINT      NOT NULL COMMENT '患者用户ID',
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM/CONFIRMED/CANCELLED',
    doctor_note          VARCHAR(1000)        DEFAULT NULL COMMENT '医生说明',
    patient_confirm_time DATETIME             DEFAULT NULL COMMENT '患者确认时间',
    create_time          DATETIME             DEFAULT NULL,
    update_time          DATETIME             DEFAULT NULL,
    deleted              TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_med_advice_session (session_id),
    KEY idx_med_advice_user (user_id, create_time),
    KEY idx_med_advice_doctor (doctor_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生用药建议单';

CREATE TABLE IF NOT EXISTS medication_advice_item (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    advice_id      BIGINT       NOT NULL COMMENT '用药建议单ID',
    medicine_id    BIGINT       NOT NULL COMMENT '药品ID',
    medicine_name  VARCHAR(100) NOT NULL COMMENT '药品名称快照',
    specification  VARCHAR(100)          DEFAULT NULL COMMENT '规格快照',
    usage_method   VARCHAR(100)          DEFAULT NULL COMMENT '用法',
    dosage         VARCHAR(100)          DEFAULT NULL COMMENT '剂量',
    frequency      VARCHAR(100)          DEFAULT NULL COMMENT '频次',
    duration_days  INT          NOT NULL DEFAULT 1 COMMENT '用药天数',
    quantity       VARCHAR(100)          DEFAULT NULL COMMENT '数量',
    precautions    VARCHAR(1000)         DEFAULT NULL COMMENT '注意事项',
    sort_order     INT          NOT NULL DEFAULT 1 COMMENT '排序',
    create_time    DATETIME              DEFAULT NULL,
    update_time    DATETIME              DEFAULT NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_med_item_advice (advice_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药建议明细';

INSERT INTO medicine (name, generic_name, brand_name, category, dosage_form, specification, unit, default_usage, default_dosage, default_frequency, default_duration_days, max_duration_days, indications, contraindications, precautions, adverse_reactions, requires_offline, status, create_time, update_time, deleted)
SELECT '布洛芬缓释胶囊', '布洛芬', NULL, '解热镇痛', '胶囊', '0.3g*20粒', '盒', '口服', '一次1粒', '每日2次', 3, 5, '用于缓解轻至中度疼痛、发热等症状。', '对布洛芬或其他非甾体抗炎药过敏者禁用；活动性消化道溃疡患者慎用。', '饭后服用；如持续高热、疼痛加重或出现胃痛黑便等情况请及时线下就医。', '可见胃肠不适、皮疹等。', 0, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM medicine WHERE name = '布洛芬缓释胶囊' AND deleted = 0);

INSERT INTO medicine (name, generic_name, brand_name, category, dosage_form, specification, unit, default_usage, default_dosage, default_frequency, default_duration_days, max_duration_days, indications, contraindications, precautions, adverse_reactions, requires_offline, status, create_time, update_time, deleted)
SELECT '氯雷他定片', '氯雷他定', NULL, '抗过敏', '片剂', '10mg*12片', '盒', '口服', '一次1片', '每日1次', 3, 7, '用于缓解过敏性鼻炎、荨麻疹等相关症状。', '对本品过敏者禁用。', '服药期间如出现明显嗜睡、心悸或症状持续不缓解，请线下就医。', '偶见乏力、嗜睡、口干等。', 0, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM medicine WHERE name = '氯雷他定片' AND deleted = 0);

INSERT INTO medicine (name, generic_name, brand_name, category, dosage_form, specification, unit, default_usage, default_dosage, default_frequency, default_duration_days, max_duration_days, indications, contraindications, precautions, adverse_reactions, requires_offline, status, create_time, update_time, deleted)
SELECT '蒙脱石散', '蒙脱石', NULL, '消化系统', '散剂', '3g*10袋', '盒', '口服', '一次1袋', '每日3次', 2, 3, '用于成人及儿童急、慢性腹泻的辅助处理。', '肠梗阻、严重便秘者慎用。', '需与其他药物间隔服用；若出现发热、脓血便、脱水等情况请及时线下就医。', '可能引起便秘。', 0, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM medicine WHERE name = '蒙脱石散' AND deleted = 0);

INSERT INTO medicine (name, generic_name, brand_name, category, dosage_form, specification, unit, default_usage, default_dosage, default_frequency, default_duration_days, max_duration_days, indications, contraindications, precautions, adverse_reactions, requires_offline, status, create_time, update_time, deleted)
SELECT '口服补液盐III', '口服补液盐', NULL, '补液电解质', '散剂', '5.125g*6袋', '盒', '按说明冲服', '按说明书配制后少量多次饮用', '按需', 1, 3, '用于腹泻、呕吐后补充水分和电解质。', '严重脱水、意识异常、无法口服者需线下处理。', '必须按说明比例冲调，不要自行加糖或浓缩；老人儿童脱水明显应及时就医。', '偶见恶心、腹胀等。', 0, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM medicine WHERE name = '口服补液盐III' AND deleted = 0);

INSERT INTO medicine (name, generic_name, brand_name, category, dosage_form, specification, unit, default_usage, default_dosage, default_frequency, default_duration_days, max_duration_days, indications, contraindications, precautions, adverse_reactions, requires_offline, status, create_time, update_time, deleted)
SELECT '阿莫西林胶囊', '阿莫西林', NULL, '抗感染', '胶囊', '0.25g*24粒', '盒', '口服', '遵医嘱', '遵医嘱', 0, 0, '用于敏感菌感染，需医生结合病情判断。', '青霉素过敏者禁用。', '抗菌药物需线下评估或处方资质确认后使用，不建议在线上直接开具。', '可见皮疹、胃肠不适，严重过敏反应需立即就医。', 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM medicine WHERE name = '阿莫西林胶囊' AND deleted = 0);
