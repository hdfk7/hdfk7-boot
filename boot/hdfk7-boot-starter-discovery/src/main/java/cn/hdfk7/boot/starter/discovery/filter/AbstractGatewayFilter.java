package cn.hdfk7.boot.starter.discovery.filter;

import cn.hdfk7.boot.starter.discovery.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGatewayFilter implements GlobalFilter, Ordered {
    protected static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    protected static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

    protected final AppProperties appProperties;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!shouldFilter(request)) {
            return chain.filter(exchange);
        }

        ServerWebExchange mutatedExchange = this.filling(exchange);
        return Mono.deferContextual(contextView -> chain.filter(mutatedExchange));
    }

    protected boolean shouldFilter(ServerHttpRequest request) {
        URI requestUri = request.getURI();
        String schema = requestUri.getScheme();
        return ("http".equals(schema) || "https".equals(schema)) && !checkExcludeUri(requestUri.getPath());
    }

    protected boolean checkExcludeUri(String uri) {
        String urlWhitelist = appProperties.getUrlWhitelist();
        if (!StringUtils.hasText(urlWhitelist)) {
            return false;
        }
        return Arrays.stream(urlWhitelist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(item -> ANT_PATH_MATCHER.match(item, uri));
    }

    protected ServerWebExchange filling(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
