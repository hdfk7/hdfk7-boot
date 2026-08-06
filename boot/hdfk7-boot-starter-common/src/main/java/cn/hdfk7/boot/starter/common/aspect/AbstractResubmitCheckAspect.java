package cn.hdfk7.boot.starter.common.aspect;

import cn.hdfk7.boot.proto.base.annotation.ResubmitCheck;
import cn.hdfk7.boot.proto.base.exception.ResubmitException;
import cn.hdfk7.boot.starter.common.properties.HttpHeaderProperties;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Slf4j
public abstract class AbstractResubmitCheckAspect implements Ordered {
    protected static final int ORDER = 1;

    private final HttpHeaderProperties httpHeaderProperties;
    private final RedissonClient redissonClient;
    private final String applicationName;

    protected AbstractResubmitCheckAspect(HttpHeaderProperties httpHeaderProperties, RedissonClient redissonClient, String applicationName) {
        this.httpHeaderProperties = httpHeaderProperties;
        this.redissonClient = redissonClient;
        this.applicationName = applicationName;
    }

    public Object doTask(ProceedingJoinPoint joinPoint, ResubmitCheck resubmitCheck) throws Throwable {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            return joinPoint.proceed();
        }

        String token = httpHeaderProperties.getTokenName();
        HttpServletRequest request = sra.getRequest();
        String authorization = request.getHeader(token);
        if (StrUtil.isEmpty(authorization)) {
            authorization = request.getParameter(token);
        }
        if (StrUtil.isEmpty(authorization)) {
            return joinPoint.proceed();
        }

        String methodType = request.getMethod();
        if (Arrays.stream(resubmitCheck.methods()).noneMatch(o -> o == RequestMethod.resolve(methodType))) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        Map<String, String[]> map = request.getParameterMap();
        StringBuilder sb = new StringBuilder();
        sb.append(request.getRequestURL()).append(methodType).append(methodName).append(authorization);
        map.forEach((k, v) -> {
            sb.append(k);
            if (v != null) {
                sb.append(String.join("", v));
            }
        });

        log.debug("resubmit check {}", authorization);
        String key = String.format("resubmit_check:%s:%s", applicationName, SecureUtil.md5(sb.toString()));
        RBucket<Boolean> cooldownMarker = redissonClient.getBucket(key + ":cooldown");
        if (cooldownMarker.isExists()) {
            throw new ResubmitException();
        }
        RLock lock = redissonClient.getLock(key);
        if (!lock.tryLock()) {
            throw new ResubmitException();
        }
        try {
            if (cooldownMarker.isExists()) {
                throw new ResubmitException();
            }
            try {
                return joinPoint.proceed();
            } finally {
                long cooldownMillis = resubmitCheck.cooldownMillis();
                if (cooldownMillis > 0) {
                    try {
                        cooldownMarker.set(true, Duration.ofMillis(cooldownMillis));
                    } catch (Exception e) {
                        log.error("Failed to set resubmit cooldown marker, key={}", key, e);
                    }
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
