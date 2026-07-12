-- 已有数据库单独补充医生账号时执行本脚本。
-- 前提：doctor 表已包含 user_id 和 audit_status 字段；否则先执行 doctor_consult_upgrade.sql。
USE hda_db;

-- 以 doctor 表主键顺序生成 doctor1、doctor2 ...，密码统一为 123456。
INSERT INTO sys_user (username, password, nickname, role, points, status, gender, create_time, update_time, deleted)
SELECT CONCAT('doctor', ranked.seq),
       '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS',
       ranked.name, 'DOCTOR', 0, 0, 0, NOW(), NOW(), 0
FROM (
    SELECT id, name, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
role = 'DOCTOR',
status = 0,
deleted = 0,
update_time = NOW();

UPDATE doctor d
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked ON ranked.id = d.id
JOIN sys_user u ON u.username = CONCAT('doctor', ranked.seq)
SET d.user_id = u.id, d.audit_status = 'APPROVED', d.status = 1;

SELECT d.id, d.name, u.username, d.audit_status, d.status
FROM doctor d
JOIN sys_user u ON u.id = d.user_id
WHERE u.username LIKE 'doctor%'
ORDER BY d.id;
