CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '商户订单号',
    external_id VARCHAR(64) NOT NULL COMMENT '平台商户号',
    total_amount NUMERIC(18, 2) NOT NULL COMMENT '订单总金额',
    subject VARCHAR(128) NOT NULL COMMENT '订单标题',
    client_ip VARCHAR(64) NOT NULL COMMENT '用户客户端IP',
    type_index INTEGER COMMENT '支付类型：1=手机网站，2=电脑网站，3=应用内支付，4=小程序，5=公众号',
    goods_type INTEGER COMMENT '商品类型：允许1-9，当前默认1，具体含义以平台定义为准',
    pay_method_type VARCHAR(32) COMMENT '支付方式：ALIPAY_CN=支付宝，ALIPAY=国际支付宝，WECHATPAY=微信支付，CARD=银行卡',
    attach_info VARCHAR(512) COMMENT '商户附加信息',
    return_url VARCHAR(512) COMMENT '支付完成前端跳转地址',
    quit_url VARCHAR(512) COMMENT '退出支付跳转地址',
    sub_external_id VARCHAR(64) COMMENT '子商户号',
    status VARCHAR(32) NOT NULL COMMENT '本地订单状态：CREATED=订单已创建，CREATE_SUCCESS=下单成功，CREATE_FAILED=下单失败，SUCCESS=交易成功，FINISHED=交易结束，CLOSED=交易关闭，UNKNOWN_NOTIFY=未知通知状态',
    trade_status VARCHAR(64) COMMENT '平台交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭',
    plat_trade_no VARCHAR(128) COMMENT '平台交易单号',
    third_out_trade_no VARCHAR(128) COMMENT '第三方交易单号',
    pay_url VARCHAR(2048) COMMENT '平台支付链接',
    evoke_mode VARCHAR(16) COMMENT '支付唤起模式：0=跳转链接，1=表单提交，2=二维码',
    platform_create_response LONGTEXT COMMENT '平台创建订单原始响应',
    notify_payload LONGTEXT COMMENT '最近一次支付通知原始参数',
    paid_at TIMESTAMP COMMENT '交易成功时间',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
    KEY idx_payment_order_plat_trade_no (plat_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单表';

CREATE TABLE IF NOT EXISTS payment_notify_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) COMMENT '商户订单号',
    external_id VARCHAR(64) COMMENT '平台商户号',
    trade_status VARCHAR(64) COMMENT '平台交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭',
    platform_out_trade_no VARCHAR(128) COMMENT '平台交易单号',
    notify_key VARCHAR(255) NOT NULL COMMENT '通知幂等唯一键',
    verified BOOLEAN NOT NULL COMMENT '验签结果：1=true=通过，0=false=失败',
    result VARCHAR(32) NOT NULL COMMENT '通知处理结果：SUCCESS=处理成功，FAIL=处理失败',
    failure_reason VARCHAR(512) COMMENT '通知处理失败原因',
    notify_payload LONGTEXT COMMENT '平台通知原始参数',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_payment_notify_key (notify_key),
    KEY idx_payment_notify_order_no (order_no),
    KEY idx_payment_notify_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付通知记录表';

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
    display_name VARCHAR(64) NOT NULL COMMENT '管理员显示名称',
    enabled BOOLEAN NOT NULL COMMENT '账号状态：1=true=启用，0=false=禁用',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理后台账号表';

CREATE TABLE IF NOT EXISTS admin_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    admin_user_id BIGINT NOT NULL COMMENT '管理员账号ID',
    token VARCHAR(128) NOT NULL COMMENT '登录会话令牌',
    expires_at TIMESTAMP NOT NULL COMMENT '会话过期时间',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_admin_session_token (token),
    KEY idx_admin_session_user_id (admin_user_id),
    KEY idx_admin_session_expires_at (expires_at),
    CONSTRAINT fk_admin_session_user FOREIGN KEY (admin_user_id) REFERENCES admin_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理后台登录会话表';

CREATE TABLE IF NOT EXISTS payment_refund (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    request_no VARCHAR(64) NOT NULL COMMENT '本地退款请求单号',
    order_no VARCHAR(64) NOT NULL COMMENT '商户订单号',
    plat_trade_no VARCHAR(128) NOT NULL COMMENT '平台交易单号',
    refund_amount NUMERIC(18, 2) NOT NULL COMMENT '退款金额',
    refund_reason VARCHAR(256) NOT NULL COMMENT '退款原因',
    status VARCHAR(32) NOT NULL COMMENT '本地退款状态：PROCESSING=处理中，SUCCESS=退款成功，FAILED=退款失败',
    trade_status VARCHAR(64) COMMENT '平台退款交易状态：保存平台返回的原始枚举值',
    platform_response LONGTEXT COMMENT '平台退款原始响应',
    created_by VARCHAR(64) NOT NULL COMMENT '提交退款的管理员账号',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_payment_refund_request_no (request_no),
    KEY idx_payment_refund_order_no (order_no),
    KEY idx_payment_refund_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';
