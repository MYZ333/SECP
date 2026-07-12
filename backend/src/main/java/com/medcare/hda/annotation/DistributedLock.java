package com.medcare.hda.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁（Redisson）。关键"先查后写"操作加锁，防并发重复/超卖。
 * key 支持 SpEL，可引用方法参数，如：key = "'exchange:' + #userId"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** 锁 key（SpEL 表达式，最终 key 为 hda:lock:{解析值}） */
    String key();

    /** 等待获取锁的最长时间（秒），超时视为繁忙 */
    long waitSeconds() default 3;

    /** 持锁后自动释放时间（秒），防死锁 */
    long leaseSeconds() default 10;

    /** 获取锁失败提示 */
    String message() default "操作太频繁，请稍后再试";
}
