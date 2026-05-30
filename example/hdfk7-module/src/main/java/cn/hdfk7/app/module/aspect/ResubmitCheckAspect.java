package cn.hdfk7.app.module.aspect;

import cn.hdfk7.boot.proto.base.annotation.ResubmitCheck;
import cn.hdfk7.boot.starter.common.aspect.AbstractResubmitCheckAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ResubmitCheckAspect extends AbstractResubmitCheckAspect {
    @Around("@within(resubmitCheck) || @annotation(resubmitCheck)")
    public Object doAround(ProceedingJoinPoint joinPoint, ResubmitCheck resubmitCheck) throws Throwable {
        return doTask(joinPoint, resubmitCheck);
    }
}
