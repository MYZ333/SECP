package com.medcare.hda.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等：客户端先调 /api/idempotent/token 领取一次性 token，
 * 提交时放到请求头（默认 Idempotency-Key），服务端原子消费，重复提交被拦。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 携带幂等 token 的请求头名 */
    String header() default "Idempotency-Key";

    /** 重复提交时的提示 */
    String message() default "请勿重复提交";
}
