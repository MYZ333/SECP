package com.medcare.hda.aspect;

import com.medcare.hda.annotation.DistributedLock;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/** 分布式锁切面：解析 @DistributedLock 的 SpEL key，用 Redisson 加锁执行 */
@Slf4j
@Aspect
@Order(10) // 先于 @Transactional(默认最低优先级)执行，保证"加锁 → 开事务 → 提交 → 释放锁"
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "hda:lock:";
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_PREFIX + resolveKey(pjp, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(distributedLock.waitSeconds(),
                    distributedLock.leaseSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ResultCode.LOCK_ACQUIRE_FAIL.getCode(), distributedLock.message());
            }
            return pjp.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String resolveKey(ProceedingJoinPoint pjp, String spel) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        Object[] args = pjp.getArgs();
        EvaluationContext ctx = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        Expression expression = parser.parseExpression(spel);
        Object value = expression.getValue(ctx);
        return String.valueOf(value);
    }
}
