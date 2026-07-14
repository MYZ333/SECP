USE hda_db;

-- Existing databases run this script once. It only adds the long-term-memory structures.
CREATE TABLE IF NOT EXISTS long_term_memory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    memory_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    visibility VARCHAR(24) NOT NULL,
    source_agent VARCHAR(24) NOT NULL,
    source_session_id VARCHAR(36) DEFAULT NULL,
    source_trace_id VARCHAR(36) DEFAULT NULL,
    confidence DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    version_no INT NOT NULL DEFAULT 1,
    vector_id VARCHAR(64) DEFAULT NULL,
    index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_long_memory_id (memory_id),
    KEY idx_long_memory_user (user_id, deleted, update_time),
    KEY idx_long_memory_user_category (user_id, category, visibility, deleted),
    KEY idx_long_memory_hash (user_id, content_hash, deleted),
    KEY idx_long_memory_index (index_status, retry_count, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户级共享长期记忆';

CREATE TABLE IF NOT EXISTS long_term_memory_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    memory_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    old_value_json JSON DEFAULT NULL,
    new_value_json JSON DEFAULT NULL,
    actor_type VARCHAR(24) NOT NULL,
    source_trace_id VARCHAR(36) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_long_memory_history (user_id, memory_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期记忆变更历史';

CREATE TABLE IF NOT EXISTS long_term_memory_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    source_agent VARCHAR(24) NOT NULL,
    source_session_id VARCHAR(36) DEFAULT NULL,
    source_trace_id VARCHAR(36) DEFAULT NULL,
    user_message TEXT NOT NULL,
    assistant_answer MEDIUMTEXT DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME DEFAULT NULL,
    last_error VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_long_memory_job (job_id),
    KEY idx_long_memory_job_retry (status, next_retry_time, retry_count),
    KEY idx_long_memory_job_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期记忆异步抽取任务';
