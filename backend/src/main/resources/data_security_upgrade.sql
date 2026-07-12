-- ============================================================
-- 敏感字段加密：列扩容脚本
-- 加密后为 "ENC:" + Base64(密文)，长度显著大于明文，需把相关列改为 TEXT。
-- 执行顺序：先备份数据库 → 跑本脚本改列 → 启动时设 hda.crypto.migrate=true 加密回填 → 改回 false。
-- 备份示例：mysqldump -u root -p --default-character-set=utf8mb4 hda_db > hda_db_backup.sql
-- ============================================================

USE hda_db;

-- 就诊/用药记录：诊断、处方 加密存储
ALTER TABLE medical_record MODIFY diagnosis    TEXT COMMENT '诊断(AES加密存储)';
ALTER TABLE medical_record MODIFY prescription TEXT COMMENT '处方/用药(AES加密存储)';

-- 健康档案：过敏史、家族史、既往史 加密存储
ALTER TABLE health_profile MODIFY allergy_history TEXT COMMENT '过敏史(AES加密存储)';
ALTER TABLE health_profile MODIFY family_history  TEXT COMMENT '家族病史(AES加密存储)';
ALTER TABLE health_profile MODIFY past_history    TEXT COMMENT '既往病史(AES加密存储)';

-- 说明：
-- 1) 手机号(sys_user.phone) 因参与唯一索引 uk_phone 与登录等值查询，本轮采用"出参脱敏"(138****8000)，
--    不做落库加密。如需彻底加密存储，须改为确定性加密并新增 phone_hash 列做等值检索，属独立一轮改造。
-- 2) 迁移是幂等的：已加密行再次迁移不会重复加密。
