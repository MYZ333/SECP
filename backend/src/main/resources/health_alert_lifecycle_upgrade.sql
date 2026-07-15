-- 仅用于已经执行过旧版 health_alert_upgrade.sql、表中已有 generation_key 的环境。
-- 从原始 schema 直接升级时不要执行本文件，应执行完整版 health_alert_upgrade.sql。
ALTER TABLE health_alert
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/IGNORED' AFTER read_flag,
    ADD COLUMN trigger_count INT NOT NULL DEFAULT 1 COMMENT '当前预警累计触发次数' AFTER status,
    ADD COLUMN latest_metric_id BIGINT DEFAULT NULL COMMENT '最近触发体征ID' AFTER trigger_count,
    ADD COLUMN last_trigger_time DATETIME DEFAULT NULL COMMENT '最近触发时间' AFTER latest_metric_id,
    ADD COLUMN handling_channel VARCHAR(30) DEFAULT NULL COMMENT 'HEALTH_ASSISTANT/DOCTOR_CONSULT' AFTER last_trigger_time,
    ADD COLUMN related_session_id VARCHAR(64) DEFAULT NULL COMMENT '关联咨询会话ID' AFTER handling_channel,
    ADD COLUMN resolved_time DATETIME DEFAULT NULL COMMENT '解决或忽略时间' AFTER related_session_id,
    ADD COLUMN resolution_note VARCHAR(500) DEFAULT NULL COMMENT '解决说明或忽略原因' AFTER resolved_time,
    ADD KEY idx_alert_user_status (user_id, status, last_trigger_time);

UPDATE health_alert
SET status = CASE WHEN read_flag = 1 THEN 'ACKNOWLEDGED' ELSE 'OPEN' END,
    trigger_count = 1,
    last_trigger_time = COALESCE(last_trigger_time, create_time);
