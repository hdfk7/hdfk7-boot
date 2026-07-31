package cn.hdfk7.boot.starter.common.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

        if (throwable instanceof ResponseStatusException responseStatusException) {
            response.setStatusCode(responseStatusException.getStatusCode());
        }

        return response.writeWith(Mono.fromSupplier(() -> this.buildResponseBuffer(response, request, throwable)));
    }

    protected DataBuffer buildResponseBuffer(ServerHttpResponse response, ServerHttpRequest request, Throwable throwable) {
        String message = this.resolveResultMessage(response, request, throwable);
        int resultCode = this.resolveResultCode(throwable);
        this.writeExceptionLog(response, request, message, throwable);

        String body = JSONUtil.toJsonStr(ResultCode.SYS_ERROR.bindResult()
                .bindMsg(message)
                .bindCode(resultCode));
        DataBufferFactory bufferFactory = response.bufferFactory();
        return bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));
    }

    protected int resolveResultCode(Throwable throwable) {
        if (throwable instanceof BaseException baseException) {
            return baseException.code.getCode();
        }
        return ResultCode.SYS_ERROR.getCode();
    }

    protected String resolveResultMessage(ServerHttpResponse response, ServerHttpRequest request, Throwable throwable) {
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            return String.format("No handler found for %s %s",
                    request.getMethod().name().toUpperCase(Locale.ROOT),
                    request.getURI().getPath());
        }
        if (StrUtil.isEmpty(throwable.getMessage())) {
            return ResultCode.SYS_ERROR.getMsg();
        }
        return throwable.getMessage();
    }

    protected void writeExceptionLog(ServerHttpResponse response, ServerHttpRequest request, String message, Throwable throwable) {
        String httpMethod = request.getMethod().name().toUpperCase(Locale.ROOT);
        String url = request.getURI().getPath();
        if (isWarnException(response, throwable)) {
            log.warn("{}:{} {}", httpMethod, url, message);
        } else {
            log.error("{}:{} {}", httpMethod, url, message, throwable);
        }
    }

    protected boolean isWarnException(ServerHttpResponse response, Throwable throwable) {
        return response.getStatusCode() == HttpStatus.NOT_FOUND
                || throwable instanceof ResponseStatusException
                || throwable instanceof BaseException;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
