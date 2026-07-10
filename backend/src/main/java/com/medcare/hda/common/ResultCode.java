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

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    OLD_PASSWORD_ERROR(1004, "原密码错误"),
    POINTS_NOT_ENOUGH(2001, "积分不足"),
    STOCK_NOT_ENOUGH(2002, "库存不足");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
