-- Seed or refresh doctor login accounts for an existing database.
USE hda_db;

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

SELECT d.id, d.name, u.username, d.audit_status, d.status
FROM doctor d
JOIN sys_user u ON u.id = d.user_id
WHERE u.username LIKE 'doctor%'
ORDER BY d.id;
