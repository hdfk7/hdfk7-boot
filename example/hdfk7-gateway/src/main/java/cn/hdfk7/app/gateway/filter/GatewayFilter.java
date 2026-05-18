package cn.hdfk7.app.gateway.filter;

import cn.hdfk7.app.gateway.component.properties.ApplicationProperties;
import cn.hdfk7.boot.starter.common.constants.HttpHeaderConst;
import cn.hutool.core.util.StrUtil;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayFilter implements GlobalFilter, Ordered {
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private final ApplicationProperties applicationProperties;
    private final Tracer tracer;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI requestUri = request.getURI();
        String schema = requestUri.getScheme();
        if (((!"http".equals(schema) && !"https".equals(schema))) || checkExcludeUri(request.getURI().getPath())) {
            return chain.filter(exchange);
        }
        ServerWebExchange mutatedExchange = filling(exchange);
        return Mono.deferContextual(contextView -> {
            String traceId = Optional.of(tracer.currentTraceContext())
                    .map(CurrentTraceContext::context)
                    .map(TraceContext::traceId)
                    .orElse("");
            mutatedExchange.getResponse().getHeaders().set(HttpHeaderConst.TID, traceId);
            return chain.filter(mutatedExchange);
        });
    }

    private boolean checkExcludeUri(String uri) {
        String urlWhitelist = applicationProperties.getUrlWhitelist();
        if (StrUtil.isEmpty(urlWhitelist)) {
            return Boolean.FALSE;
        }
        return Arrays.stream(urlWhitelist.split(",")).anyMatch(item -> ANT_PATH_MATCHER.match(item, uri));
    }

    private ServerWebExchange filling(ServerWebExchange exchange) {
        ServerHttpRequest host = exchange.getRequest().mutate()
                .build();
        return exchange.mutate().request(host).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
