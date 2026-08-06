package cn.hdfk7.app.service.adapter.web.aspect;

import cn.hdfk7.boot.proto.base.annotation.ResubmitCheck;
import cn.hdfk7.boot.starter.common.aspect.AbstractResubmitCheckAspect;
import cn.hdfk7.boot.starter.common.properties.HttpHeaderProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ResubmitCheckAspect extends AbstractResubmitCheckAspect {
    public ResubmitCheckAspect(HttpHeaderProperties httpHeaderProperties, RedissonClient redissonClient, @Value("${spring.application.name:}") String applicationName) {
        super(httpHeaderProperties, redissonClient, applicationName);
    }

    @Around("@within(resubmitCheck) || @annotation(resubmitCheck)")
    public Object doAround(ProceedingJoinPoint joinPoint, ResubmitCheck resubmitCheck) throws Throwable {
        return doTask(joinPoint, resubmitCheck);
    }
}
