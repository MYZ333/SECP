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
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0正常1禁用',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    -- 手机号唯一（NULL 不受限）。已有库升级请执行：
    -- ALTER TABLE sys_user ADD UNIQUE KEY uk_phone (phone);
    -- 若报重复键错误，先查重：SELECT phone, COUNT(*) FROM sys_user WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*)>1;
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------
-- RBAC-角色
-- ---------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
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

-- ---------------------------
-- RBAC-账号角色关联
-- ---------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
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

-- ---------------------------
-- 患者基础资料(仅 PATIENT 身份)
-- ---------------------------
DROP TABLE IF EXISTS patient_profile;
CREATE TABLE patient_profile (
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

-- ---------------------------
-- 积分账户(仅 PATIENT 身份)
-- ---------------------------
DROP TABLE IF EXISTS point_account;
CREATE TABLE point_account (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT NOT NULL COMMENT '账号ID',
    balance     INT    NOT NULL DEFAULT 0 COMMENT '积分余额',
    create_time DATETIME         DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME         DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户';

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
    -- 长文本字段 2000 字符（老库升级：ALTER TABLE health_profile MODIFY allergy_history VARCHAR(2000), MODIFY family_history VARCHAR(2000), MODIFY past_history VARCHAR(2000), MODIFY remark VARCHAR(2000);）
    allergy_history   VARCHAR(2000)         DEFAULT NULL COMMENT '过敏史',
    family_history    VARCHAR(2000)         DEFAULT NULL COMMENT '家族病史',
    past_history      VARCHAR(2000)         DEFAULT NULL COMMENT '既往病史',
    emergency_contact VARCHAR(50)           DEFAULT NULL COMMENT '紧急联系人',
    emergency_phone   VARCHAR(20)           DEFAULT NULL COMMENT '紧急联系电话',
    remark            VARCHAR(2000)         DEFAULT NULL COMMENT '备注',
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
    category     VARCHAR(50)           DEFAULT NULL COMMENT '类别:健康监测/医疗服务/康复护理/营养保健/生活家居',
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
    user_id      BIGINT                DEFAULT NULL COMMENT '关联登录用户ID',
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    avatar       VARCHAR(255)          DEFAULT NULL COMMENT '头像',
    phone        VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    title        VARCHAR(50)           DEFAULT NULL COMMENT '职称',
    hospital     VARCHAR(100)          DEFAULT NULL COMMENT '医院',
    department   VARCHAR(50)           DEFAULT NULL COMMENT '科室',
    speciality   VARCHAR(255)          DEFAULT NULL COMMENT '擅长',
    introduction VARCHAR(1000)         DEFAULT NULL COMMENT '简介',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0停用1启用',
    audit_status VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态:PENDING/APPROVED/REJECTED',
    create_time  DATETIME              DEFAULT NULL,
    update_time  DATETIME              DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_department (department),
    UNIQUE KEY uk_doctor (name, hospital, department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生专家库';

-- ---------------------------
-- 医生咨询会话
-- ---------------------------
DROP TABLE IF EXISTS doctor_consult_session;
CREATE TABLE doctor_consult_session (
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

-- ---------------------------
-- 医生咨询消息
-- ---------------------------
DROP TABLE IF EXISTS doctor_consult_message;
CREATE TABLE doctor_consult_message (
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
INSERT INTO sys_role (code, name, status, create_time, update_time, deleted)
VALUES
('ADMIN', '管理员', 1, NOW(), NOW(), 0),
('PATIENT', '患者', 1, NOW(), NOW(), 0),
('DOCTOR', '医生', 1, NOW(), NOW(), 0);

INSERT INTO sys_user (username, password, nickname, status, create_time, update_time, deleted)
VALUES
('admin',   '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS', '系统管理员', 0, NOW(), NOW(), 0),
('user001', '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS', '张大爷',     0, NOW(), NOW(), 0);

INSERT INTO sys_user_role (user_id, role_id, create_time, update_time, deleted)
SELECT u.id, r.id, NOW(), NOW(), 0
FROM sys_user u
JOIN sys_role r ON (u.username = 'admin' AND r.code = 'ADMIN')
                OR (u.username = 'user001' AND r.code = 'PATIENT');

INSERT INTO patient_profile (user_id, gender, create_time, update_time, deleted)
SELECT id, 1, NOW(), NOW(), 0 FROM sys_user WHERE username = 'user001';

INSERT INTO point_account (user_id, balance, create_time, update_time, deleted)
SELECT id, 100, NOW(), NOW(), 0 FROM sys_user WHERE username = 'user001';

INSERT INTO point_product (name, category, description, points_cost, stock, status, create_time, update_time, deleted)
VALUES
-- 健康监测
('电子血压计',     '健康监测', '家用臂式全自动电子血压计',           500, 20, 1, NOW(), NOW(), 0),
('血糖试纸',       '健康监测', '血糖仪配套试纸50片',                 150, 100,1, NOW(), NOW(), 0),
('家用血糖仪',     '健康监测', '免调码血糖仪套装(含采血笔)',         400, 30, 1, NOW(), NOW(), 0),
('指夹式血氧仪',   '健康监测', '血氧饱和度/脉率双测量, 大屏显示',    260, 40, 1, NOW(), NOW(), 0),
('电子体温计',     '健康监测', '医用级电子体温计, 30秒快速测温',     80,  120,1, NOW(), NOW(), 0),
('智能健康手环',   '健康监测', '心率/睡眠监测, 跌倒提醒, 超长续航',  600, 25, 1, NOW(), NOW(), 0),
-- 医疗服务
('体检套餐券',     '医疗服务', '老年人基础体检套餐',                 800, 10, 1, NOW(), NOW(), 0),
('专家挂号协助券', '医疗服务', '三甲医院专家号预约协助服务1次',      300, 30, 1, NOW(), NOW(), 0),
('中医推拿体验券', '医疗服务', '肩颈腰腿中医推拿45分钟1次',          350, 20, 1, NOW(), NOW(), 0),
('上门护理服务券', '医疗服务', '执业护士居家上门基础护理1次',        900, 10, 1, NOW(), NOW(), 0),
('口腔检查券',     '医疗服务', '口腔全景检查+洁牙基础套餐',          450, 15, 1, NOW(), NOW(), 0),
-- 康复护理
('颈椎按摩仪',     '康复护理', '恒温热敷+多档揉捏, 便携颈部按摩',    550, 20, 1, NOW(), NOW(), 0),
('电动足浴盆',     '康复护理', '恒温加热, 气泡按摩, 安全防漏电',     480, 15, 1, NOW(), NOW(), 0),
('四脚防滑拐杖',   '康复护理', '铝合金可调高度, 四脚稳固防滑',       220, 40, 1, NOW(), NOW(), 0),
('保暖护膝套装',   '康复护理', '自发热护膝一对, 秋冬关节保暖',       120, 80, 1, NOW(), NOW(), 0),
-- 营养保健
('钙维D软胶囊',    '营养保健', '碳酸钙+维生素D3, 60粒/瓶',           200, 60, 1, NOW(), NOW(), 0),
('深海鱼油胶囊',   '营养保健', 'Omega-3深海鱼油, 100粒/瓶',          320, 40, 1, NOW(), NOW(), 0),
('中老年蛋白粉',   '营养保健', '乳清蛋白+大豆蛋白, 900g罐装',        420, 30, 1, NOW(), NOW(), 0),
('复合维生素片',   '营养保健', '中老年专用复合维生素矿物质, 90片',   250, 50, 1, NOW(), NOW(), 0),
-- 生活家居
('健康枕头',       '生活家居', '颈椎护理记忆棉枕头',                 200, 50, 1, NOW(), NOW(), 0),
('防蓝光老花镜',   '生活家居', '轻量树脂镜片, 多度数可选',           150, 60, 1, NOW(), NOW(), 0),
('智能保温杯',     '生活家居', '316不锈钢, 触控显温, 500ml',         180, 70, 1, NOW(), NOW(), 0),
('浴室防滑垫',     '生活家居', '加大加厚吸盘防滑垫, 淋浴房适用',     90,  100,1, NOW(), NOW(), 0),
('智能控温电热毯', '生活家居', '双区控温, 定时断电保护',             380, 25, 1, NOW(), NOW(), 0);

INSERT INTO doctor (name, title, hospital, department, speciality, introduction, status, create_time, update_time, deleted)
VALUES
('李明华', '主任医师',   '市第一人民医院', '心血管内科', '高血压、冠心病、心力衰竭', '从事心血管临床工作30年，主持省级课题3项，累计完成介入手术4000余例。', 1, NOW(), NOW(), 0),
('王秀英', '副主任医师', '市中医院',       '内分泌科',   '糖尿病、甲状腺疾病、骨质疏松', '擅长老年糖尿病综合管理与中西医结合治疗，从业22年。', 1, NOW(), NOW(), 0),
('赵国强', '主任医师',   '市老年病医院',   '老年医学科', '老年综合评估、多重用药管理、慢病管理', '专注老年医养结合服务，牵头建立市级老年综合评估中心。', 1, NOW(), NOW(), 0),
('陈建平', '主任医师',   '市第一人民医院', '心血管内科', '心律失常、房颤射频消融、起搏器植入', '心脏电生理专家，年完成射频消融手术600余台。', 1, NOW(), NOW(), 0),
('刘芳',   '主治医师',   '市第二人民医院', '内分泌科',   '2型糖尿病、妊娠糖尿病、肥胖症', '专注糖尿病个体化用药与胰岛素泵管理，从业12年。', 1, NOW(), NOW(), 0),
('张伟民', '主任医师',   '省人民医院',     '神经内科',   '脑卒中、帕金森病、认知障碍', '国家卒中中心核心成员，擅长脑血管病急救与康复一体化管理。', 1, NOW(), NOW(), 0),
('孙丽娟', '副主任医师', '省人民医院',     '神经内科',   '头痛、眩晕、睡眠障碍', '专注老年睡眠障碍与慢性头痛诊疗，发表核心期刊论文20余篇。', 1, NOW(), NOW(), 0),
('周天成', '主任医师',   '市骨科医院',     '骨科',       '骨质疏松、腰椎间盘突出、关节置换', '完成髋膝关节置换手术3000余例，擅长老年骨折微创治疗。', 1, NOW(), NOW(), 0),
('吴桂兰', '副主任医师', '市骨科医院',     '骨科',       '颈肩腰腿痛、骨关节炎保守治疗', '擅长老年退行性骨关节病的阶梯化非手术治疗。', 1, NOW(), NOW(), 0),
('郑海涛', '主任医师',   '市第一人民医院', '呼吸内科',   '慢阻肺、哮喘、肺部感染', '呼吸危重症专家，市呼吸疾病质控中心副主任。', 1, NOW(), NOW(), 0),
('黄玉梅', '主治医师',   '市第二人民医院', '呼吸内科',   '慢性咳嗽、睡眠呼吸暂停', '专注老年慢性气道疾病的长期随访管理，从业10年。', 1, NOW(), NOW(), 0),
('徐立新', '主任医师',   '市中医院',       '中医科',     '中医体质调理、脾胃病、失眠', '第五批全国老中医药专家学术经验继承人，从业28年。', 1, NOW(), NOW(), 0),
('马淑华', '副主任医师', '市中医院',       '中医科',     '针灸推拿、中风后康复、面瘫', '擅长针药结合治疗老年中风后遗症，年门诊量1.2万人次。', 1, NOW(), NOW(), 0),
('高志远', '主任医师',   '省肿瘤医院',     '肿瘤科',     '肺癌、消化道肿瘤的综合治疗', '省抗癌协会常务理事，擅长老年肿瘤患者的个体化综合治疗。', 1, NOW(), NOW(), 0),
('林静',   '主治医师',   '市第一人民医院', '消化内科',   '胃炎、胃溃疡、幽门螺杆菌、便秘', '完成胃肠镜检查1万余例，擅长老年消化道疾病内镜诊疗。', 1, NOW(), NOW(), 0),
('罗永强', '副主任医师', '市第一人民医院', '消化内科',   '脂肪肝、肝硬化、胆石症', '专注慢性肝病全程管理，从业18年。', 1, NOW(), NOW(), 0),
('宋雅琴', '副主任医师', '市妇幼保健院',   '眼科',       '白内障、青光眼、老年黄斑变性', '完成白内障超声乳化手术5000余例，擅长老年眼病筛查与手术。', 1, NOW(), NOW(), 0),
('冯建国', '主任医师',   '市老年病医院',   '老年医学科', '阿尔茨海默病、老年营养、跌倒预防', '主编《老年综合征管理手册》，深耕老年痴呆早期干预15年。', 1, NOW(), NOW(), 0),
('钱美玲', '副主任医师', '市康复医院',     '康复医学科', '偏瘫康复、骨折术后康复、慢性疼痛', '擅长老年脑卒中与骨折术后的系统康复训练，从业16年。', 1, NOW(), NOW(), 0),
('谢文斌', '主任医师',   '市第二人民医院', '泌尿外科',   '前列腺增生、泌尿系结石、尿失禁', '擅长老年前列腺疾病微创手术，累计完成手术2000余例。', 1, NOW(), NOW(), 0);

-- 按医生记录顺序分配 doctor1、doctor2 ... 登录账号，密码均为 123456。
INSERT INTO sys_user (username, password, nickname, status, create_time, update_time, deleted)
SELECT CONCAT('doctor', ranked.seq),
       '$2b$10$D2ZEKoZtHLLfSbUPrLBZkeev.sOTaYzMIrQeKhRERdLW6G2tkbYZS',
       ranked.name, 0, NOW(), NOW(), 0
FROM (
    SELECT id, name, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked;

INSERT INTO sys_user_role (user_id, role_id, create_time, update_time, deleted)
SELECT u.id, r.id, NOW(), NOW(), 0
FROM sys_user u
JOIN sys_role r ON r.code = 'DOCTOR'
WHERE u.username LIKE 'doctor%';

UPDATE doctor d
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq
    FROM doctor
    WHERE deleted = 0
) ranked ON ranked.id = d.id
JOIN sys_user u ON u.username = CONCAT('doctor', ranked.seq)
SET d.user_id = u.id, d.audit_status = 'APPROVED', d.status = 1;

-- ---------------------------
-- AI / RAG / 长期记忆模块完整重建
-- schema.sql 是项目唯一初始化入口：每次执行均清空旧会话、知识库元数据、审计和长期记忆。
-- 按业务依赖从明细表到主表删除，避免旧数据或旧表结构残留。
-- ---------------------------
DROP TABLE IF EXISTS long_term_memory_job;
DROP TABLE IF EXISTS long_term_memory_history;
DROP TABLE IF EXISTS long_term_memory;
DROP TABLE IF EXISTS agent_chat_turn;
DROP TABLE IF EXISTS agent_run_step;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS agent_consult_state;
DROP TABLE IF EXISTS knowledge_chunk;
DROP TABLE IF EXISTS knowledge_document;
DROP TABLE IF EXISTS SPRING_AI_CHAT_MEMORY;
DROP TABLE IF EXISTS agent_chat_session;

-- 健康助手 Agent 会话授权索引（消息正文由下方 SPRING_AI_CHAT_MEMORY 保存）
CREATE TABLE agent_chat_session (
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

-- Spring AI 1.0.9 JDBC 持久化对话记忆表
CREATE TABLE SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT NOT NULL,
    type            ENUM('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL,
    timestamp       TIMESTAMP NOT NULL,
    INDEX SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX (conversation_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Spring AI 持久化对话记忆';

CREATE TABLE knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_type VARCHAR(20) NOT NULL DEFAULT 'HEALTH',
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

CREATE TABLE knowledge_chunk (
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

CREATE TABLE agent_consult_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    episode_id VARCHAR(36) NOT NULL,
    phase VARCHAR(20) NOT NULL DEFAULT 'COLLECTING',
    round_count INT NOT NULL DEFAULT 0,
    initial_question TEXT NOT NULL,
    clinical_summary MEDIUMTEXT DEFAULT NULL,
    known_facts_json JSON DEFAULT NULL,
    missing_fields_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consult_state_user_session (user_id, session_id),
    KEY idx_consult_state_phase (phase, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康助手多轮问诊状态';

CREATE TABLE agent_run (
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

CREATE TABLE agent_run_step (
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

CREATE TABLE agent_chat_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    answer MEDIUMTEXT NOT NULL,
    risk_level VARCHAR(20) DEFAULT NULL,
    citations_json JSON DEFAULT NULL,
    profile_categories_json JSON DEFAULT NULL,
    doctor_recommendations_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_turn_trace (trace_id),
    KEY idx_agent_turn_session (user_id, session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康智能体结构化对话轮次';

-- 用户级共享长期记忆。MySQL 是事实源，Chroma 仅作为语义索引。
CREATE TABLE long_term_memory (
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

CREATE TABLE long_term_memory_history (
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

CREATE TABLE long_term_memory_job (
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
