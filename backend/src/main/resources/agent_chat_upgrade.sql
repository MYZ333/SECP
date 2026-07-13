-- 健康助手 Agent 会话授权索引。
-- 消息正文由 Spring AI JDBC ChatMemoryRepository 写入 SPRING_AI_CHAT_MEMORY。
USE hda_db;

CREATE TABLE IF NOT EXISTS agent_chat_session (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    session_id      VARCHAR(36)  NOT NULL,
    conversation_id VARCHAR(36)  NOT NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_session (user_id, session_id),
    UNIQUE KEY uk_conversation_id (conversation_id),
    KEY idx_user_update_time (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康助手会话授权索引';

-- 与 Spring AI 1.0.9 JdbcChatMemoryRepository 的 MySQL 表结构保持一致。
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT NOT NULL,
    type            ENUM('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL,
    timestamp       TIMESTAMP NOT NULL,
    INDEX SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX (conversation_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Spring AI 持久化对话记忆';

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    source_org VARCHAR(120) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    published_date DATE DEFAULT NULL,
    version_no VARCHAR(60) DEFAULT NULL,
    category VARCHAR(60) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    chunk_count INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1000) DEFAULT NULL,
    reviewer_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_checksum (checksum),
    KEY idx_knowledge_status (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康知识库文档';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_no INT NOT NULL,
    section_title VARCHAR(255) DEFAULT NULL,
    content MEDIUMTEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    vector_id VARCHAR(64) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_chunk (document_id, chunk_no),
    KEY idx_chunk_document_status (document_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康知识库切片';

-- 兼容项目早期知识库表结构：只重命名/补列，不删除旧数据。
-- MySQL 不支持在 CHANGE COLUMN 上直接使用 IF EXISTS，因此用 information_schema 做条件迁移。
DELIMITER $$
DROP PROCEDURE IF EXISTS hda_rename_column_if_needed$$
CREATE PROCEDURE hda_rename_column_if_needed(
    IN p_table VARCHAR(64), IN p_old VARCHAR(64), IN p_new VARCHAR(64), IN p_definition VARCHAR(255)
)
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name=p_table AND column_name=p_old)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name=p_table AND column_name=p_new) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` CHANGE COLUMN `', p_old, '` `', p_new, '` ', p_definition);
        PREPARE hda_stmt FROM @ddl; EXECUTE hda_stmt; DEALLOCATE PREPARE hda_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS hda_add_column_if_needed$$
CREATE PROCEDURE hda_add_column_if_needed(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema=DATABASE() AND table_name=p_table AND column_name=p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE hda_stmt FROM @ddl; EXECUTE hda_stmt; DEALLOCATE PREPARE hda_stmt;
    END IF;
END$$
DELIMITER ;

CALL hda_rename_column_if_needed('knowledge_document', 'name', 'title', 'VARCHAR(200) NOT NULL');
CALL hda_rename_column_if_needed('knowledge_document', 'source', 'source_url', 'VARCHAR(1000) DEFAULT NULL');
CALL hda_rename_column_if_needed('knowledge_document', 'source_updated_at', 'published_date', 'DATE DEFAULT NULL');
CALL hda_rename_column_if_needed('knowledge_document', 'version', 'version_no', 'VARCHAR(60) DEFAULT NULL');
CALL hda_add_column_if_needed('knowledge_document', 'source_org', 'VARCHAR(120) NOT NULL DEFAULT ''历史导入''');
CALL hda_add_column_if_needed('knowledge_document', 'file_name', 'VARCHAR(255) NOT NULL DEFAULT ''legacy-document.txt''');
CALL hda_add_column_if_needed('knowledge_document', 'file_path', 'VARCHAR(500) NOT NULL DEFAULT ''''');
CALL hda_add_column_if_needed('knowledge_document', 'checksum', 'VARCHAR(64) NOT NULL DEFAULT ''''');
CALL hda_add_column_if_needed('knowledge_document', 'chunk_count', 'INT NOT NULL DEFAULT 0');
CALL hda_add_column_if_needed('knowledge_document', 'failure_reason', 'VARCHAR(1000) DEFAULT NULL');
CALL hda_add_column_if_needed('knowledge_document', 'reviewer_id', 'BIGINT DEFAULT NULL');
CALL hda_add_column_if_needed('knowledge_document', 'agent_type', 'VARCHAR(20) NOT NULL DEFAULT ''HEALTH''');
UPDATE knowledge_document SET agent_type='HEALTH' WHERE agent_type IS NULL OR agent_type='';

CALL hda_rename_column_if_needed('knowledge_chunk', 'chunk_index', 'chunk_no', 'INT NOT NULL');
CALL hda_rename_column_if_needed('knowledge_chunk', 'title', 'section_title', 'VARCHAR(255) DEFAULT NULL');
CALL hda_rename_column_if_needed('knowledge_chunk', 'vector_ref', 'vector_id', 'VARCHAR(64) DEFAULT NULL');
CALL hda_add_column_if_needed('knowledge_chunk', 'checksum', 'VARCHAR(64) NOT NULL DEFAULT ''''');
CALL hda_add_column_if_needed('knowledge_chunk', 'status', 'VARCHAR(20) NOT NULL DEFAULT ''DRAFT''');

-- 旧表的整篇 content 字段保留用于数据追溯，但新文档按 chunk 存储，因此允许为空。
SET @has_legacy_content = (SELECT COUNT(*) FROM information_schema.columns
                           WHERE table_schema=DATABASE() AND table_name='knowledge_document' AND column_name='content');
SET @ddl = IF(@has_legacy_content > 0,
              'ALTER TABLE knowledge_document MODIFY COLUMN content LONGTEXT NULL',
              'SELECT 1');
PREPARE hda_stmt FROM @ddl; EXECUTE hda_stmt; DEALLOCATE PREPARE hda_stmt;

DROP PROCEDURE IF EXISTS hda_rename_column_if_needed;
DROP PROCEDURE IF EXISTS hda_add_column_if_needed;

CREATE TABLE IF NOT EXISTS agent_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trace_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    route VARCHAR(100) DEFAULT NULL,
    risk_level VARCHAR(20) DEFAULT NULL,
    use_health_profile TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    latency_ms BIGINT DEFAULT NULL,
    error_code VARCHAR(80) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_trace (trace_id),
    KEY idx_agent_user_session (user_id, session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康智能体运行审计';

CREATE TABLE IF NOT EXISTS agent_run_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trace_id VARCHAR(36) NOT NULL,
    agent_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary VARCHAR(1000) DEFAULT NULL,
    latency_ms BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_agent_step_trace (trace_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康智能体执行步骤';

CREATE TABLE IF NOT EXISTS agent_chat_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    answer MEDIUMTEXT NOT NULL,
    risk_level VARCHAR(20) DEFAULT NULL,
    citations_json JSON DEFAULT NULL,
    profile_categories_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_turn_trace (trace_id),
    KEY idx_agent_turn_session (user_id, session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康智能体结构化对话轮次';
