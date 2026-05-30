package cn.hdfk7.app.module.controller.exception;

import cn.hdfk7.boot.starter.common.aspect.AbstractLogAspect;
import cn.hdfk7.boot.starter.common.exception.AbstractGlobalExceptionHandler;
import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {
    public GlobalExceptionHandler(AbstractLogAspect logAspect, ClientIpResolver clientIpResolver) {
        super(logAspect, clientIpResolver);
    }
}
