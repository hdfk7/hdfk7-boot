package cn.hdfk7.boot.starter.common.sentinel;

import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnClass(value = {BlockRequestHandler.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class FluxBlockExceptionHandler implements BlockRequestHandler {

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(JSONUtil.toJsonStr(ResultCode.SERVICE_DOWNGRADE_ERROR.bindResult())));
    }

}
