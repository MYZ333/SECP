-- Upgrade an existing database from sys_user.role / sys_user.points to RBAC tables.
USE hda_db;

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(30) NOT NULL COMMENT '角色编码:ADMIN/PATIENT/DOCTOR',
    name        VARCHAR(50) NOT NULL COMMENT '角色名称',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态:0停用1启用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT  NOT NULL COMMENT '账号ID',
    role_id     BIGINT  NOT NULL COMMENT '角色ID',
    create_time DATETIME         DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME         DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关联表';

CREATE TABLE IF NOT EXISTS patient_profile (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT  NOT NULL COMMENT '账号ID',
    gender      TINYINT          DEFAULT 0 COMMENT '性别:0未知1男2女',
    birthday    DATE             DEFAULT NULL COMMENT '生日',
    create_time DATETIME         DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME         DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者基础资料';

CREATE TABLE IF NOT EXISTS point_account (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT NOT NULL COMMENT '账号ID',
    balance     INT    NOT NULL DEFAULT 0 COMMENT '积分余额',
    create_time DATETIME         DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME         DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户';

INSERT INTO sys_role (code, name, status, create_time, update_time, deleted)
VALUES
('ADMIN', '管理员', 1, NOW(), NOW(), 0),
('PATIENT', '患者', 1, NOW(), NOW(), 0),
('DOCTOR', '医生', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE status = 1, update_time = NOW(), deleted = 0;

INSERT INTO sys_user_role (user_id, role_id, create_time, update_time, deleted)
SELECT u.id,
       CASE
           WHEN u.role = 'ADMIN' THEN admin_role.id
           WHEN u.role = 'DOCTOR' THEN doctor_role.id
           ELSE patient_role.id
       END,
       NOW(), NOW(), 0
FROM sys_user u
JOIN sys_role admin_role ON admin_role.code = 'ADMIN'
JOIN sys_role patient_role ON patient_role.code = 'PATIENT'
JOIN sys_role doctor_role ON doctor_role.code = 'DOCTOR'
WHERE u.deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW();

INSERT INTO patient_profile (user_id, gender, birthday, create_time, update_time, deleted)
SELECT u.id, u.gender, u.birthday, NOW(), NOW(), 0
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
JOIN sys_role r ON r.id = ur.role_id AND r.code = 'PATIENT'
WHERE u.deleted = 0
ON DUPLICATE KEY UPDATE gender = VALUES(gender), birthday = VALUES(birthday), update_time = NOW(), deleted = 0;

INSERT INTO point_account (user_id, balance, create_time, update_time, deleted)
SELECT u.id, u.points, NOW(), NOW(), 0
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
JOIN sys_role r ON r.id = ur.role_id AND r.code = 'PATIENT'
WHERE u.deleted = 0
ON DUPLICATE KEY UPDATE balance = VALUES(balance), update_time = NOW(), deleted = 0;

-- After verifying the application on the upgraded database, drop old columns manually:
-- ALTER TABLE sys_user DROP COLUMN role, DROP COLUMN points, DROP COLUMN gender, DROP COLUMN birthday;
