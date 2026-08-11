package cn.hdfk7.boot.starter.common.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hdfk7.boot.starter.common.aspect.AbstractLogAspect;
import cn.hdfk7.boot.starter.common.constants.HttpHeaderConst;
import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGlobalExceptionHandler implements Ordered {
    protected static final int ORDER = -1;

    private final AbstractLogAspect logAspect;
    private final ClientIpResolver clientIpResolver;

    @ExceptionHandler(value = Exception.class)
    public Result<?> handler(Exception exception) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String message = this.resolveResultMessage(exception);
        Result<Object> result = this.buildResult(exception, message);
        if (requestAttributes == null) {
            return result;
        }

        HttpServletRequest request = requestAttributes.getRequest();
        this.writeExceptionLog(request, message, exception);
        logAspect.finishTask(result);
        return result;
    }

    protected String resolveResultMessage(Exception exception) {
        if (exception instanceof NoResourceFoundException) {
            return "404 NOT_FOUND";
        }
        if (exception instanceof BindException bindException) {
            return this.resolveBindMessage(bindException);
        }
        if (isWarnException(exception)) {
            return exception.getMessage();
        }
        return ResultCode.SYS_ERROR.getMsg();
    }

    protected String resolveBindMessage(BindException bindException) {
        List<FieldError> errors = bindException.getBindingResult().getFieldErrors();
        StringBuilder errorMessage = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            FieldError error = errors.get(i);
            errorMessage.append("[")
                    .append(error.getField())
                    .append("]")
                    .append(error.getDefaultMessage());
            if (i < errors.size() - 1) {
                errorMessage.append(",");
            }
        }
        return errorMessage.toString();
    }

    protected Result<Object> buildResult(Exception exception, String message) {
        if (exception instanceof BaseException baseException) {
            return baseException.getResultCode().toResult(baseException.getErrorData(), message);
        }
        return ResultCode.SYS_ERROR.toResult(null, message);
    }

    protected void writeExceptionLog(HttpServletRequest request, String message, Exception exception) {
        String method = request.getMethod();
        String url = request.getRequestURL().toString();
        String host = clientIpResolver.getIpAddress(request.getHeader(HttpHeaderConst.X_REAL_IP), request.getRemoteAddr());
        int port = request.getRemotePort();
        if (isWarnException(exception)) {
            log.warn("method={},url={},host={},port={},msg={}", method, url, host, port, message);
        } else {
            log.error("method={},url={},host={},port={}", method, url, host, port, exception);
        }
    }

    protected boolean isWarnException(Exception exception) {
        return exception instanceof BindException
                || exception instanceof ConstraintViolationException
                || exception instanceof HttpRequestMethodNotSupportedException
                || exception instanceof NoResourceFoundException
                || exception instanceof BaseException;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
