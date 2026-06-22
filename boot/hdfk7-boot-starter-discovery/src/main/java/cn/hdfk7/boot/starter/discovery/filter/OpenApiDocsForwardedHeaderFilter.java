package cn.hdfk7.boot.starter.discovery.filter;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;

@RequiredArgsConstructor
public class OpenApiDocsForwardedHeaderFilter implements HttpHeadersFilter, Ordered {
    protected static final int ORDER = Ordered.LOWEST_PRECEDENCE;

    protected static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    protected static final String X_FORWARDED_PORT = "X-Forwarded-Port";
    protected static final String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";
    protected static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    protected final DiscoveryClient discoveryClient;
    protected final NacosServiceLookup nacosServiceLookup;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    protected String apiDocsPath;

    @Override
    public @NonNull HttpHeaders filter(@NonNull HttpHeaders input, @NonNull ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String serviceId = serviceId(exchange);
        if (!StringUtils.hasText(serviceId) || !isRegisteredService(serviceId)) {
            return input;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(input);
        setForwardedHeaders(headers, request, serviceId);
        return headers;
    }

    protected void setForwardedHeaders(HttpHeaders headers, ServerHttpRequest request, String serviceId) {
        setIfAbsent(headers, X_FORWARDED_PREFIX, "/" + serviceId);
        setIfAbsent(headers, X_FORWARDED_HOST, requestHost(request));
        setIfAbsent(headers, X_FORWARDED_PROTO, request.getURI().getScheme());
        setIfAbsent(headers, X_FORWARDED_PORT, requestPort(request));
    }

    protected void setIfAbsent(HttpHeaders headers, String name, String value) {
        if (!StringUtils.hasText(firstHeader(headers, name)) && StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    protected String requestHost(ServerHttpRequest request) {
        if (request.getHeaders().getHost() != null) {
            return request.getHeaders().getHost().getHostString();
        }
        return request.getURI().getHost();
    }

    protected String requestPort(ServerHttpRequest request) {
        if (request.getHeaders().getHost() != null && request.getHeaders().getHost().getPort() > 0) {
            return String.valueOf(request.getHeaders().getHost().getPort());
        }
        return defaultPort(request.getURI());
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

    protected String serviceId(ServerWebExchange exchange) {
        String serviceId = serviceId(exchange.getRequest().getPath().pathWithinApplication().value());
        if (StringUtils.hasText(serviceId)) {
            return serviceId;
        }

        LinkedHashSet<URI> originalRequestUrls = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        if (originalRequestUrls == null) {
            return null;
        }

        return originalRequestUrls.stream()
                .map(URI::getPath)
                .map(this::serviceId)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    protected String serviceId(String path) {
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
        String serviceIdLowerCase = serviceId.toLowerCase(Locale.ROOT);
        return discoveryClient.getServices()
                .stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(serviceIdLowerCase::equals)
                && nacosServiceLookup.hasHealthyInstances(serviceId);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
