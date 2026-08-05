package cn.hdfk7.boot.starter.common.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
public abstract class AbstractGatewayExceptionHandler implements ErrorWebExceptionHandler, Ordered {
    protected static final int ORDER = -1;

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();

        HttpHeaders httpHeaders = response.getHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        return response.writeWith(Mono.fromSupplier(() -> this.buildResponseBuffer(response, request, throwable)));
    }

    protected DataBuffer buildResponseBuffer(ServerHttpResponse response, ServerHttpRequest request, Throwable throwable) {
        String message = this.resolveResultMessage(request, throwable);
        Result<Object> result = this.buildResult(throwable, message);
        this.writeExceptionLog(request, message, throwable);

        String body = JSONUtil.toJsonStr(result);
        DataBufferFactory bufferFactory = response.bufferFactory();
        return bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));
    }

    protected String resolveResultMessage(ServerHttpRequest request, Throwable throwable) {
        if (throwable instanceof ResponseStatusException) {
            return String.format("No handler found for %s %s", request.getMethod().name().toUpperCase(Locale.ROOT), request.getURI().getPath());
        }
        if (isWarnException(throwable)) {
            return throwable.getMessage();
        }
        return ResultCode.SYS_ERROR.getMsg();
    }

    protected Result<Object> buildResult(Throwable throwable, String message) {
        if (throwable instanceof BaseException baseException) {
            return ResultCode.SYS_ERROR.bindResult(baseException.errorData)
                    .bindMsg(message)
                    .bindCode(baseException.code.getCode());
        }
        return ResultCode.SYS_ERROR.bindResult()
                .bindMsg(message)
                .bindCode(ResultCode.SYS_ERROR.getCode());
    }

    protected void writeExceptionLog(ServerHttpRequest request, String message, Throwable throwable) {
        String httpMethod = request.getMethod().name().toUpperCase(Locale.ROOT);
        String url = request.getURI().getPath();
        if (isWarnException(throwable)) {
            log.warn("{}:{} {}", httpMethod, url, message);
        } else {
            log.error("{}:{} {}", httpMethod, url, message, throwable);
        }
    }

    protected boolean isWarnException(Throwable throwable) {
        return throwable instanceof ResponseStatusException
                || throwable instanceof BaseException;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
