package com.medcare.hda.common.ratelimit;

/** 限流维度 */
public enum LimitDimension {
    /** 按客户端 IP 限流（未登录接口，如登录/注册） */
    IP,
    /** 按登录用户 ID 限流（已登录接口，如 AI 咨询） */
    USER,
    /** 全局限流（整个接口共用一个计数） */
    GLOBAL
}
