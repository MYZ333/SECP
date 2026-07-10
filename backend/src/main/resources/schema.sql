-- =====================================================================
-- 智慧医养大数据公共服务平台 - 个人健康档案系统  建表脚本 (MySQL 8)
-- 使用方法：在 Navicat/命令行中执行本脚本即可创建数据库、表与初始数据
-- 默认账号：管理员 admin / 123456 ，普通用户 user001 / 123456
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS hda_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE hda_db;

-- ---------------------------
-- 用户表
-- ---------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    nickname     VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    avatar       VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    phone        VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    gender       TINYINT               DEFAULT 0 COMMENT '性别:0未知1男2女',
    birthday     DATE                  DEFAULT NULL COMMENT '生日',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色:USER/ADMIN',
    points       INT          NOT NULL DEFAULT 0 COMMENT '积分余额',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0正常1禁用',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------
-- 健康档案-基本信息
-- ---------------------------
DROP TABLE IF EXISTS health_profile;
CREATE TABLE health_profile (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL COMMENT '用户ID',
    height            DOUBLE                DEFAULT NULL COMMENT '身高cm',
    weight            DOUBLE                DEFAULT NULL COMMENT '体重kg',
    blood_type        VARCHAR(10)           DEFAULT NULL COMMENT '血型',
    allergy_history   VARCHAR(500)          DEFAULT NULL COMMENT '过敏史',
    family_history    VARCHAR(500)          DEFAULT NULL COMMENT '家族病史',
    past_history      VARCHAR(500)          DEFAULT NULL COMMENT '既往病史',
    emergency_contact VARCHAR(50)           DEFAULT NULL COMMENT '紧急联系人',
    emergency_phone   VARCHAR(20)           DEFAULT NULL COMMENT '紧急联系电话',
    remark            VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    create_time       DATETIME              DEFAULT NULL,
    update_time       DATETIME              DEFAULT NULL,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案-基本信息';

-- ---------------------------
-- 健康档案-体征/体检数据
-- ---------------------------
DROP TABLE IF EXISTS health_metric;
CREATE TABLE health_metric (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    metric_type   VARCHAR(30)  NOT NULL COMMENT '指标类型',
    metric_value  DOUBLE                DEFAULT NULL COMMENT '指标值',
    metric_value2 DOUBLE                DEFAULT NULL COMMENT '第二数值(如舒张压)',
    unit          VARCHAR(20)           DEFAULT NULL COMMENT '单位',
    measure_time  DATETIME              DEFAULT NULL COMMENT '测量时间',
    abnormal      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否异常:0正常1异常',
    remark        VARCHAR(255)          DEFAULT NULL,
    create_time   DATETIME              DEFAULT NULL,
    update_time   DATETIME              DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_type (user_id, metric_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案-体征/体检数据';

-- ---------------------------
-- 健康档案-就诊/用药记录
-- ---------------------------
DROP TABLE IF EXISTS medical_record;
CREATE TABLE medical_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    visit_date   DATE                  DEFAULT NULL COMMENT '就诊日期',
    hospital     VARCHAR(100)          DEFAULT NULL COMMENT '就诊医院',
    department   VARCHAR(50)           DEFAULT NULL COMMENT '科室',
    doctor_name  VARCHAR(50)           DEFAULT NULL COMMENT '医生',
    diagnosis    VARCHAR(500)          DEFAULT NULL COMMENT '诊断',
    prescription VARCHAR(1000)         DEFAULT NULL COMMENT '处方/用药',
    remark       VARCHAR(500)          DEFAULT NULL,
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案-就诊/用药记录';

-- ---------------------------
-- 健康档案-健康报告
-- ---------------------------
DROP TABLE IF EXISTS health_report;
CREATE TABLE health_report (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    title        VARCHAR(100) NOT NULL COMMENT '报告标题',
    report_type  VARCHAR(20)           DEFAULT 'OTHER' COMMENT '类型:PHYSICAL/AI/OTHER',
    report_date  DATE                  DEFAULT NULL COMMENT '报告日期',
    content      TEXT                  COMMENT '报告内容/结论',
    file_url     VARCHAR(255)          DEFAULT NULL COMMENT '附件URL',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案-健康报告';

-- ---------------------------
-- 积分明细
-- ---------------------------
DROP TABLE IF EXISTS point_record;
CREATE TABLE point_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    change_points INT          NOT NULL COMMENT '变动积分(正得负耗)',
    balance       INT          NOT NULL COMMENT '变动后余额',
    type          VARCHAR(20)  NOT NULL COMMENT '类型',
    description   VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    create_time   DATETIME              DEFAULT NULL,
    update_time   DATETIME              DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分明细';

-- ---------------------------
-- 积分商城商品
-- ---------------------------
DROP TABLE IF EXISTS point_product;
CREATE TABLE point_product (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL COMMENT '商品名称',
    image        VARCHAR(255)          DEFAULT NULL COMMENT '图片URL',
    description  VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    points_cost  INT          NOT NULL COMMENT '所需积分',
    stock        INT          NOT NULL DEFAULT 0 COMMENT '库存',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0下架1上架',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城商品';

-- ---------------------------
-- 积分兑换记录
-- ---------------------------
DROP TABLE IF EXISTS point_exchange;
CREATE TABLE point_exchange (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    product_id   BIGINT       NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100)          DEFAULT NULL COMMENT '商品名称快照',
    points_cost  INT          NOT NULL COMMENT '消耗积分',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '数量',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待发货1已发货2完成3取消',
    address      VARCHAR(255)          DEFAULT NULL COMMENT '收货信息',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换记录';

-- ---------------------------
-- 医生专家库
-- ---------------------------
DROP TABLE IF EXISTS doctor;
CREATE TABLE doctor (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    avatar       VARCHAR(255)          DEFAULT NULL COMMENT '头像',
    title        VARCHAR(50)           DEFAULT NULL COMMENT '职称',
    hospital     VARCHAR(100)          DEFAULT NULL COMMENT '医院',
    department   VARCHAR(50)           DEFAULT NULL COMMENT '科室',
    speciality   VARCHAR(255)          DEFAULT NULL COMMENT '擅长',
    introduction VARCHAR(1000)         DEFAULT NULL COMMENT '简介',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0停用1启用',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生专家库';

-- ---------------------------
-- 健康咨询记录(AI)
-- ---------------------------
DROP TABLE IF EXISTS consult_record;
CREATE TABLE consult_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    session_id   VARCHAR(64)           DEFAULT NULL COMMENT '会话ID',
    role         VARCHAR(20)  NOT NULL COMMENT 'user/assistant',
    content      TEXT                  COMMENT '消息内容',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_session (user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康咨询记录';

-- ---------------------------
-- 健康预警(AI)
-- ---------------------------
DROP TABLE IF EXISTS health_alert;
CREATE TABLE health_alert (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    level        VARCHAR(10)           DEFAULT 'LOW' COMMENT '级别:LOW/MEDIUM/HIGH',
    alert_type   VARCHAR(50)           DEFAULT NULL COMMENT '预警类型',
    content      TEXT                  COMMENT '预警内容/AI建议',
    read_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读:0未读1已读',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康预警';

-- =====================================================================
-- 初始化数据 (密码均为 123456 的 BCrypt 值)
-- =====================================================================
INSERT INTO sys_user (username, password, nickname, role, points, status, gender, create_time, update_time, deleted)
VALUES
('admin',   '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS', '系统管理员', 'ADMIN', 0,   0, 1, NOW(), NOW(), 0),
('user001', '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS', '张大爷',     'USER',  100, 0, 1, NOW(), NOW(), 0);

INSERT INTO point_product (name, description, points_cost, stock, status, create_time, update_time, deleted)
VALUES
('电子血压计',   '家用臂式全自动电子血压计', 500, 20, 1, NOW(), NOW(), 0),
('体检套餐券',   '老年人基础体检套餐',       800, 10, 1, NOW(), NOW(), 0),
('健康枕头',     '颈椎护理记忆棉枕头',       200, 50, 1, NOW(), NOW(), 0),
('血糖试纸',     '血糖仪配套试纸50片',       150, 100,1, NOW(), NOW(), 0);

INSERT INTO doctor (name, title, hospital, department, speciality, introduction, status, create_time, update_time, deleted)
VALUES
('李明华', '主任医师', '市第一人民医院', '心血管内科', '高血压、冠心病、心力衰竭', '从事心血管临床工作30年。', 1, NOW(), NOW(), 0),
('王秀英', '副主任医师', '中医院', '内分泌科', '糖尿病、甲状腺疾病', '擅长老年糖尿病综合管理。', 1, NOW(), NOW(), 0),
('赵国强', '主任医师', '老年病医院', '老年医学科', '老年综合评估、慢病管理', '专注老年医养结合服务。', 1, NOW(), NOW(), 0);
