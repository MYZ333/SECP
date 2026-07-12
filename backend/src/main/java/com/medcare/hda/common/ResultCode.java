package com.medcare.hda.common;

import lombok.Getter;

/** 统一返回状态码 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),

    // —— 认证安全 ——
    LOGIN_ELSEWHERE(1101, "账号已在其他设备登录，请重新登录"),
    TOKEN_INVALID(1102, "登录状态无效，请重新登录"),
    REFRESH_TOKEN_INVALID(1103, "登录已过期，请重新登录"),
    PASSWORD_DECRYPT_FAIL(1104, "密码解密失败，请刷新页面重试"),

    // —— 高并发 ——
    REPEAT_SUBMIT(1201, "请勿重复提交"),
    IDEMPOTENT_TOKEN_MISSING(1202, "缺少幂等令牌，请刷新后重试"),
    LOCK_ACQUIRE_FAIL(1203, "操作太频繁，请稍后再试"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    OLD_PASSWORD_ERROR(1004, "原密码错误"),
    SMS_TOO_FREQUENT(1005, "验证码发送太频繁，请60秒后再试"),
    SMS_LIMIT_EXCEEDED(1006, "发送次数已达上限，请稍后再试"),
    SMS_CODE_ERROR(1007, "验证码错误"),
    SMS_CODE_EXPIRED(1008, "验证码已过期，请重新获取"),
    SMS_CODE_INVALIDATED(1009, "错误次数过多，验证码已失效，请重新获取"),
    PHONE_NOT_REGISTERED(1010, "该手机号尚未注册"),
    SMS_SEND_FAIL(1011, "短信发送失败，请稍后再试"),
    POINTS_NOT_ENOUGH(2001, "积分不足"),
    STOCK_NOT_ENOUGH(2002, "库存不足"),
    ALREADY_CHECKED_IN(2003, "今日已签到");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
