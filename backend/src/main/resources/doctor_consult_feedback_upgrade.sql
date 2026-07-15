-- 医生咨询结束反馈闭环升级脚本
-- 已有 hda_db 执行本脚本；全新初始化可直接执行 schema.sql。
USE hda_db;

DROP PROCEDURE IF EXISTS upgrade_doctor_consult_feedback;

DELIMITER $$

CREATE PROCEDURE upgrade_doctor_consult_feedback()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'problem_overview'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN problem_overview TEXT DEFAULT NULL COMMENT '本次咨询问题概述' AFTER unread_doctor;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'preliminary_assessment'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN preliminary_assessment TEXT DEFAULT NULL COMMENT '医生初步判断' AFTER problem_overview;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'summary'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN summary TEXT DEFAULT NULL COMMENT '本次咨询总结' AFTER preliminary_assessment;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'advice'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN advice TEXT DEFAULT NULL COMMENT '后续建议' AFTER summary;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'risk_warning'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN risk_warning TEXT DEFAULT NULL COMMENT '风险提醒' AFTER advice;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'recommend_offline'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN recommend_offline TINYINT NOT NULL DEFAULT 0 COMMENT '是否建议线下就医' AFTER risk_warning;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'rating'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN rating TINYINT DEFAULT NULL COMMENT '患者评分1-5' AFTER recommend_offline;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'feedback_tags'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN feedback_tags VARCHAR(255) DEFAULT NULL COMMENT '患者评价标签' AFTER rating;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'feedback'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN feedback VARCHAR(500) DEFAULT NULL COMMENT '患者文字评价' AFTER feedback_tags;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'doctor_consult_session'
          AND COLUMN_NAME = 'feedback_time'
    ) THEN
        ALTER TABLE doctor_consult_session
            ADD COLUMN feedback_time DATETIME DEFAULT NULL COMMENT '患者评价时间' AFTER feedback;
    END IF;
END$$

DELIMITER ;

CALL upgrade_doctor_consult_feedback();
DROP PROCEDURE IF EXISTS upgrade_doctor_consult_feedback;
