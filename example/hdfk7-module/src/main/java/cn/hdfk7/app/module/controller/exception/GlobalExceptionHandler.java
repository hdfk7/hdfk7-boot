package cn.hdfk7.app.module.controller.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hdfk7.boot.starter.common.aspect.LogAspect;
import cn.hdfk7.boot.starter.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final LogAspect logAspect;

    @ExceptionHandler(value = Exception.class)
    public Result<?> handler(Exception e) {
        String msg = ResultCode.SYS_ERROR.getMsg();
        int code = ResultCode.SYS_ERROR.getCode();
        Object errorData = null;
        if (e instanceof BindException) {
            StringBuilder errorMsg = new StringBuilder();
            List<FieldError> errors;
            errors = ((BindException) e).getBindingResult().getFieldErrors();
            if (e instanceof MethodArgumentNotValidException) {
                errors = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors();
            }
            for (int i = 0; i < errors.size(); i++) {
                FieldError error = errors.get(i);
                errorMsg.append("[").append(error.getField()).append("]").append(error.getDefaultMessage()).append(i == errors.size() - 1 ? "" : ",");
            }
            msg = errorMsg.toString();
        }
        if (e instanceof ConstraintViolationException
                || e instanceof HttpRequestMethodNotSupportedException
                || e instanceof NoResourceFoundException) {
            msg = e.getMessage();
        }
        if (e instanceof BaseException) {
            msg = e.getMessage();
            code = ((BaseException) e).code.getCode();
            errorData = ((BaseException) e).errorData;
        }
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Result<Object> result = ResultCode.SYS_ERROR.bindResult(errorData).bindMsg(msg).bindCode(code);
        if (sra == null) {
            return result;
        }
        HttpServletRequest request = sra.getRequest();
        String method = sra.getRequest().getMethod();
        String url = sra.getRequest().getRequestURL().toString();
        String host = IpUtil.getIpAddress(request);
        int port = request.getRemotePort();
        log.error("method={},url={},host={},port={}", method, url, host, port, e);
        logAspect.finishTask(result);
        return result;
    }
}
