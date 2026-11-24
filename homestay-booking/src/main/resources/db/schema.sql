-- ============================================================
-- 民宿预定管理系统 表结构
-- 全部使用 IF NOT EXISTS，应用启动时会自动执行
-- ============================================================

-- 用户表：房客与房东共用，靠 role 区分
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' COMMENT 'CUSTOMER 房客 / HOST 房东',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 房型表：对外售卖单位
CREATE TABLE IF NOT EXISTS room_type (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)   NOT NULL COMMENT '房型名称',
    description  VARCHAR(500)  DEFAULT NULL COMMENT '描述',
    base_price   DECIMAL(10,2) NOT NULL COMMENT '基准价，日历未配置时兜底',
    max_guests   INT           NOT NULL DEFAULT 2 COMMENT '最多入住人数',
    bed_type     VARCHAR(30)   DEFAULT NULL COMMENT '床型，如 1.8m大床',
    area         INT           DEFAULT NULL COMMENT '面积（平方米）',
    cover_image  VARCHAR(255)  DEFAULT NULL COMMENT '封面图地址',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_room_type_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房型';

-- 物理房间表：下单后由系统自动分配
CREATE TABLE IF NOT EXISTS room (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    room_type_id BIGINT      NOT NULL COMMENT '所属房型',
    room_no      VARCHAR(20) NOT NULL COMMENT '房间号，如 201',
    floor        INT         DEFAULT NULL COMMENT '楼层',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '1 可用 0 维修中',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_no (room_no),
    KEY idx_room_type (room_type_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物理房间';

-- 日历价格与库存表：核心表，房型 x 日期 唯一
CREATE TABLE IF NOT EXISTS room_calendar (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    room_type_id BIGINT        NOT NULL COMMENT '房型',
    stay_date    DATE          NOT NULL COMMENT '入住日期（该晚）',
    price        DECIMAL(10,2) NOT NULL COMMENT '该晚价格',
    stock        INT           NOT NULL DEFAULT 0 COMMENT '该晚剩余可售间数',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1 开放 0 关闭',
    version      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_date (room_type_id, stay_date),
    KEY idx_stay_date (stay_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '日历价格与库存';

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32)   NOT NULL COMMENT '业务订单号',
    user_id        BIGINT        NOT NULL COMMENT '下单房客',
    room_type_id   BIGINT        NOT NULL COMMENT '房型',
    check_in_date  DATE          NOT NULL COMMENT '入住日期',
    check_out_date DATE          NOT NULL COMMENT '离店日期',
    nights         INT           NOT NULL COMMENT '晚数',
    room_count     INT           NOT NULL DEFAULT 1 COMMENT '间数',
    total_amount   DECIMAL(10,2) NOT NULL COMMENT '订单总额',
    status         VARCHAR(20)   NOT NULL COMMENT 'PENDING/CONFIRMED/CHECKED_IN/COMPLETED/CANCELLED',
    contact_name   VARCHAR(50)   NOT NULL COMMENT '联系人',
    contact_phone  VARCHAR(20)   NOT NULL COMMENT '联系电话',
    remark         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    cancel_reason  VARCHAR(255)  DEFAULT NULL COMMENT '取消原因',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_room_type_date (room_type_id, check_in_date, check_out_date),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';

-- 订单分配的具体房间，一单多间时有多行
CREATE TABLE IF NOT EXISTS order_room (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    room_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_room (order_id, room_id),
    KEY idx_room (room_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单房间分配';

-- 订单占用的每一个夜晚，用于取消时精确回滚库存
CREATE TABLE IF NOT EXISTS order_stay (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    order_id   BIGINT NOT NULL,
    stay_date  DATE   NOT NULL COMMENT '被占用的某一晚',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_date (order_id, stay_date),
    KEY idx_stay_date (stay_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单占用夜晚';

-- 订单状态流转日志
CREATE TABLE IF NOT EXISTS order_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_id    BIGINT      NOT NULL,
    from_status VARCHAR(20) DEFAULT NULL COMMENT '变更前状态，新建订单为空',
    to_status   VARCHAR(20) NOT NULL,
    operator_id BIGINT      DEFAULT NULL COMMENT '操作人',
    remark      VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单状态日志';
