-- 医生去重：按「姓名+医院+科室」分组，只保留 id 最小的一条，删除多余重复行。
-- 起因：INSERT 脚本被执行多次导致重复。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 hda_db < backend/src/main/resources/dedup_doctors.sql
USE hda_db;

-- 1) 先看看有哪些重复（可选，执行后核对）
SELECT name, hospital, department, COUNT(*) AS cnt
FROM doctor
GROUP BY name, hospital, department
HAVING cnt > 1;

-- 2) 删除重复，保留每组最早插入(id 最小)的一条
DELETE d1
FROM doctor d1
JOIN doctor d2
  ON d1.name = d2.name
 AND IFNULL(d1.hospital, '')   = IFNULL(d2.hospital, '')
 AND IFNULL(d1.department, '') = IFNULL(d2.department, '')
 AND d1.id > d2.id;

-- 3) 加唯一索引，防止以后再重复（如已存在同名同院同科的合法两人，请勿加此约束）
ALTER TABLE doctor ADD UNIQUE KEY uk_doctor (name, hospital, department);

-- 4) 核对最终数量（应为 20）
SELECT COUNT(*) AS total FROM doctor WHERE deleted = 0;
