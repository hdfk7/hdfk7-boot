package cn.hdfk7.boot.starter.discovery.filter;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OpenApiDocsForwardedHeaderFilter implements GlobalFilter, Ordered {
    protected static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 6;

    protected static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    protected static final String X_FORWARDED_PORT = "X-Forwarded-Port";
    protected static final String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";
    protected static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    protected final DiscoveryClient discoveryClient;
    protected final NacosServiceLookup nacosServiceLookup;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    protected String apiDocsPath;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        String serviceId = serviceId(originalRequest);
        if (!StringUtils.hasText(serviceId) || !isRegisteredService(serviceId)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = forwardedRequest(exchange, originalRequest, serviceId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    protected ServerHttpRequest forwardedRequest(ServerWebExchange exchange, ServerHttpRequest originalRequest, String serviceId) {
        return exchange.getRequest()
                .mutate()
                .headers(headers -> setForwardedHeaders(headers, originalRequest, serviceId))
                .build();
    }

    protected void setForwardedHeaders(HttpHeaders headers, ServerHttpRequest request, String serviceId) {
        setForwardedPrefix(headers, serviceId);
        setForwardedHost(headers, request);
        setForwardedProto(headers, request);
        setForwardedPort(headers, request);
    }

    protected void setForwardedPrefix(HttpHeaders headers, String serviceId) {
        if (!StringUtils.hasText(headers.getFirst(X_FORWARDED_PREFIX))) {
            headers.set(X_FORWARDED_PREFIX, "/" + serviceId);
        }
    }

    protected void setForwardedHost(HttpHeaders headers, ServerHttpRequest request) {
        String host = firstHeader(headers, X_FORWARDED_HOST);
        if (!StringUtils.hasText(host) && request.getHeaders().getHost() != null) {
            host = request.getHeaders().getHost().getHostString();
        }
        if (!StringUtils.hasText(host)) {
            host = request.getURI().getHost();
        }
        if (StringUtils.hasText(host) && !StringUtils.hasText(headers.getFirst(X_FORWARDED_HOST))) {
            headers.set(X_FORWARDED_HOST, host);
        }
    }

    protected void setForwardedProto(HttpHeaders headers, ServerHttpRequest request) {
        String proto = firstHeader(headers, X_FORWARDED_PROTO);
        if (!StringUtils.hasText(proto)) {
            proto = request.getURI().getScheme();
        }
        if (StringUtils.hasText(proto) && !StringUtils.hasText(headers.getFirst(X_FORWARDED_PROTO))) {
            headers.set(X_FORWARDED_PROTO, proto);
        }
    }

    protected void setForwardedPort(HttpHeaders headers, ServerHttpRequest request) {
        String port = firstHeader(headers, X_FORWARDED_PORT);
        if (!StringUtils.hasText(port)
                && request.getHeaders().getHost() != null
                && request.getHeaders().getHost().getPort() > 0) {
            port = String.valueOf(request.getHeaders().getHost().getPort());
        }

        if (!StringUtils.hasText(port)) {
            port = defaultPort(request.getURI());
        }

        if (StringUtils.hasText(port) && !StringUtils.hasText(headers.getFirst(X_FORWARDED_PORT))) {
            headers.set(X_FORWARDED_PORT, port);
        }
    }

    protected String firstHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.split(",", 2)[0].trim();
    }

    protected String defaultPort(URI uri) {
        if (uri.getPort() > 0) {
            return String.valueOf(uri.getPort());
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return "443";
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return "80";
        }
        return null;
    }

    protected String serviceId(ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        if (!isOpenApiDocsPath(path)) {
            return null;
        }

        String[] parts = path.split("/", 3);
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    protected boolean isOpenApiDocsPath(String path) {
        String configuredApiDocsPath = apiDocsPath();
        int apiDocsIndex = path.indexOf(configuredApiDocsPath);
        if (apiDocsIndex < 0) {
            return false;
        }
        return path.charAt(0) == '/'
                && apiDocsIndex > 1
                && (path.length() == apiDocsIndex + configuredApiDocsPath.length()
                || path.charAt(apiDocsIndex + configuredApiDocsPath.length()) == '/');
    }

    protected String apiDocsPath() {
        if (!StringUtils.hasText(apiDocsPath)) {
            return "";
        }
        return apiDocsPath.startsWith("/") ? apiDocsPath : "/" + apiDocsPath;
    }

    protected boolean isRegisteredService(String serviceId) {
        Set<String> serviceIds = discoveryClient.getServices()
                .stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return serviceIds.contains(serviceId.toLowerCase(Locale.ROOT))
                && nacosServiceLookup.hasHealthyInstances(serviceId);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
