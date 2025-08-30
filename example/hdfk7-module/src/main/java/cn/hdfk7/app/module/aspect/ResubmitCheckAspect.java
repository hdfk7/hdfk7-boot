package cn.hdfk7.app.module.aspect;

import cn.hdfk7.boot.proto.base.annotation.ResubmitCheck;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Aspect
@Component
public class ResubmitCheckAspect extends cn.hdfk7.boot.starter.common.aspect.ResubmitCheckAspect {

    @Around("@within(resubmitCheck) || @annotation(resubmitCheck)")
    public Object doAround(ProceedingJoinPoint joinPoint, ResubmitCheck resubmitCheck) throws Throwable {
        return doTask(joinPoint, resubmitCheck);
    }

}
