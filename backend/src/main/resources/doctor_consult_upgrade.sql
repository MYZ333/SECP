-- 医生端与医生咨询升级脚本
-- 已有 hda_db 执行本脚本；全新初始化可直接执行 schema.sql

USE hda_db;

ALTER TABLE doctor
    ADD COLUMN user_id BIGINT NULL COMMENT '关联登录用户ID' AFTER id,
    ADD COLUMN phone VARCHAR(20) NULL COMMENT '联系电话' AFTER avatar,
    ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态:PENDING/APPROVED/REJECTED' AFTER status,
    ADD KEY idx_user_id (user_id);

CREATE TABLE IF NOT EXISTS doctor_consult_session (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL COMMENT '患者用户ID',
    doctor_id         BIGINT      NOT NULL COMMENT '医生ID',
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    last_message      VARCHAR(500)         DEFAULT NULL COMMENT '最后一条消息',
    last_message_time DATETIME             DEFAULT NULL COMMENT '最后消息时间',
    unread_user       INT         NOT NULL DEFAULT 0 COMMENT '患者未读数',
    unread_doctor     INT         NOT NULL DEFAULT 0 COMMENT '医生未读数',
    create_time       DATETIME             DEFAULT NULL,
    update_time       DATETIME             DEFAULT NULL,
    deleted           TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, last_message_time),
    KEY idx_doctor_time (doctor_id, last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生咨询会话';

CREATE TABLE IF NOT EXISTS doctor_consult_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    session_id      BIGINT      NOT NULL COMMENT '会话ID',
    user_id         BIGINT      NOT NULL COMMENT '患者用户ID',
    doctor_id       BIGINT      NOT NULL COMMENT '医生ID',
    sender_type     VARCHAR(20) NOT NULL COMMENT 'USER/DOCTOR',
    message_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/ATTACHMENT',
    content         TEXT                 DEFAULT NULL COMMENT '消息内容',
    attachment_url  VARCHAR(255)         DEFAULT NULL COMMENT '附件URL',
    attachment_name VARCHAR(255)         DEFAULT NULL COMMENT '附件名称',
    read_flag       TINYINT     NOT NULL DEFAULT 0 COMMENT '0未读1已读',
    create_time     DATETIME             DEFAULT NULL,
    update_time     DATETIME             DEFAULT NULL,
    deleted         TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_time (session_id, create_time),
    KEY idx_user_time (user_id, create_time),
    KEY idx_doctor_time (doctor_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生咨询消息';

-- 咨询结束反馈闭环字段：医生填写总结，患者提交评价。
ALTER TABLE doctor_consult_session
    ADD COLUMN problem_overview TEXT DEFAULT NULL COMMENT '本次咨询问题概述' AFTER unread_doctor,
    ADD COLUMN preliminary_assessment TEXT DEFAULT NULL COMMENT '医生初步判断' AFTER problem_overview,
    ADD COLUMN summary TEXT DEFAULT NULL COMMENT '本次咨询总结' AFTER preliminary_assessment,
    ADD COLUMN advice TEXT DEFAULT NULL COMMENT '后续建议' AFTER summary,
    ADD COLUMN risk_warning TEXT DEFAULT NULL COMMENT '风险提醒' AFTER advice,
    ADD COLUMN recommend_offline TINYINT NOT NULL DEFAULT 0 COMMENT '是否建议线下就医' AFTER risk_warning,
    ADD COLUMN rating TINYINT DEFAULT NULL COMMENT '患者评分1-5' AFTER recommend_offline,
    ADD COLUMN feedback_tags VARCHAR(255) DEFAULT NULL COMMENT '患者评价标签' AFTER rating,
    ADD COLUMN feedback VARCHAR(500) DEFAULT NULL COMMENT '患者文字评价' AFTER feedback_tags,
    ADD COLUMN feedback_time DATETIME DEFAULT NULL COMMENT '患者评价时间' AFTER feedback;

-- 按医生记录顺序分配 doctor1、doctor2 ... 登录账号，密码均为 123456。
-- ON DUPLICATE KEY UPDATE 让账号部分可重复执行，并覆盖全部现有医生。
INSERT INTO sys_user (username, password, nickname, status, create_time, update_time, deleted)
SELECT CONCAT('doctor', ranked.seq),
       '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS',
       ranked.name, 0, NOW(), NOW(), 0
FROM (
    SELECT id, name, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
status = 0,
deleted = 0,
update_time = NOW();

INSERT INTO sys_user_role (user_id, role_id, create_time, update_time, deleted)
SELECT u.id, r.id, NOW(), NOW(), 0
FROM sys_user u
JOIN sys_role r ON r.code = 'DOCTOR'
WHERE u.username LIKE 'doctor%'
ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW();

UPDATE doctor d
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked ON ranked.id = d.id
JOIN sys_user u ON u.username = CONCAT('doctor', ranked.seq)
SET d.user_id = u.id, d.audit_status = 'APPROVED', d.status = 1;
