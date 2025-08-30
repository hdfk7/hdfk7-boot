package cn.hdfk7.boot.starter.common.aspect;

import cn.hdfk7.boot.proto.base.annotation.ResubmitCheck;
import cn.hdfk7.boot.proto.base.exception.ResubmitException;
import cn.hdfk7.boot.starter.common.properties.HttpHeaderProperties;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Map;

@Slf4j
public abstract class ResubmitCheckAspect {

    public Object doTask(ProceedingJoinPoint joinPoint, ResubmitCheck resubmitCheck) throws Throwable {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            return joinPoint.proceed();
        }

        String token = SpringUtil.getBean(HttpHeaderProperties.class).getTokenName();
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
        sb.append(request.getRequestURL()).append(methodName).append(authorization);
        map.forEach((k, v) -> {
            sb.append(k);
            if (v != null) {
                sb.append(String.join("", v));
            }
        });

        log.debug("resubmit check {}", authorization);
        String key = String.format("resubmit_check:%s:%s", SpringUtil.getApplicationName(), SecureUtil.md5(sb.toString()));
        RedissonClient redissonClient = SpringUtil.getBean(RedissonClient.class);
        RLock lock = redissonClient.getLock(key);
        if (!lock.tryLock()) {
            throw new ResubmitException();
        }
        Object ret = joinPoint.proceed();
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        return ret;
    }

}
