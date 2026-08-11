package cn.hdfk7.boot.starter.common.exception;

import cn.hdfk7.boot.proto.base.exception.BaseException;
import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hdfk7.boot.starter.common.constants.HttpHeaderConst;
import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class AbstractGatewayExceptionHandler implements ErrorWebExceptionHandler, Ordered {
    protected static final int ORDER = -1;

    private final ClientIpResolver clientIpResolver;

    protected AbstractGatewayExceptionHandler(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

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
        String message = this.resolveResultMessage(throwable);
        Result<Object> result = this.buildResult(throwable, message);
        this.writeExceptionLog(request, message, throwable);

        String body = JSONUtil.toJsonStr(result);
        DataBufferFactory bufferFactory = response.bufferFactory();
        return bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));
    }

    protected String resolveResultMessage(Throwable throwable) {
        if (throwable instanceof ResponseStatusException) {
            return "404 NOT_FOUND";
        }
        if (isWarnException(throwable)) {
            return throwable.getMessage();
        }
        return ResultCode.SYS_ERROR.getMsg();
    }

    protected Result<Object> buildResult(Throwable throwable, String message) {
        if (throwable instanceof BaseException baseException) {
            return baseException.getResultCode().toResult(baseException.getErrorData(), message);
        }
        return ResultCode.SYS_ERROR.toResult(null, message);
    }

    protected void writeExceptionLog(ServerHttpRequest request, String message, Throwable throwable) {
        String method = request.getMethod().name();
        String url = request.getURI().toString();
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String remoteHost = remoteAddress != null ? remoteAddress.getHostString() : null;
        String host = clientIpResolver.getIpAddress(request.getHeaders().getFirst(HttpHeaderConst.X_REAL_IP), remoteHost);
        int port = remoteAddress != null ? remoteAddress.getPort() : -1;
        if (isWarnException(throwable)) {
            log.warn("method={},url={},host={},port={},msg={}", method, url, host, port, message);
        } else {
            log.error("method={},url={},host={},port={}", method, url, host, port, throwable);
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
