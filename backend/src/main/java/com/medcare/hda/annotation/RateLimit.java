package com.medcare.hda.annotation;

import com.medcare.hda.common.ratelimit.LimitDimension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（Redis + Lua 滑动窗口）。
 * 用法：@RateLimit(key = "login", window = 60, limit = 5, dimension = IP)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 业务标识，参与 Redis key 组装（hda:rl:{key}:{维度值}） */
    String key();

    /** 时间窗口，单位秒 */
    int window() default 60;

    /** 窗口内允许的最大请求数 */
    int limit() default 10;

    /** 限流维度 */
    LimitDimension dimension() default LimitDimension.IP;

    /** 超限提示（为空则用统一的 RATE_LIMIT_EXCEEDED 文案） */
    String message() default "";
}
