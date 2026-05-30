package cn.hdfk7.app.module.aspect;

import cn.hdfk7.boot.starter.common.aspect.AbstractLogAspect;
import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect extends AbstractLogAspect {
    public LogAspect(ClientIpResolver clientIpResolver) {
        super(clientIpResolver);
    }

    @Pointcut("execution(public * cn.hdfk7.app.module.controller.*.*(..))")
    public void pointCutMethod() {
    }

    @Before("pointCutMethod()")
    public void doBefore(JoinPoint joinPoint) {
        init(joinPoint);
    }

    @Around("pointCutMethod()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        return doTask(joinPoint);
    }

    @AfterReturning(returning = "ret", pointcut = "pointCutMethod()")
    public void doAfterReturning(Object ret) {
        finishTask(ret);
    }
}
