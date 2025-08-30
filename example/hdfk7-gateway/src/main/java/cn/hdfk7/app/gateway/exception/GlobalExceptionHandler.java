package cn.hdfk7.app.gateway.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    @NonNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable e) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();

        HttpHeaders httpHeaders = response.getHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (response.isCommitted()) {
            return Mono.error(e);
        }

        if (e instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) e).getStatusCode());
        }

        return response
                .writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    String msg = e.getMessage();
                    int code = ResultCode.SYS_ERROR.getCode();
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        msg = String.format("No handler found for %s %s", request.getMethod().name().toUpperCase(Locale.ROOT), request.getURI().getPath());
                    }
                    if (StrUtil.isEmpty(msg)) {
                        msg = ResultCode.SYS_ERROR.getMsg();
                    }
                    if (e instanceof BaseException) {
                        code = ((BaseException) e).code.getCode();
                    }
                    String httpMethod = request.getMethod().name().toUpperCase(Locale.ROOT);
                    String url = request.getURI().getPath();
                    log.error("{}:{} {}", httpMethod, url, msg, e);
                    return bufferFactory.wrap(JSONUtil.toJsonStr(ResultCode.SYS_ERROR.bindResult().bindMsg(msg).bindCode(code)).getBytes(StandardCharsets.UTF_8));
                }));
    }
}

