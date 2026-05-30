package cn.hdfk7.app.gateway.filter;

import cn.hdfk7.boot.starter.discovery.filter.AbstractGatewayFilter;
import cn.hdfk7.boot.starter.discovery.properties.AppProperties;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class GatewayFilter extends AbstractGatewayFilter {
    public GatewayFilter(AppProperties appProperties, Tracer tracer) {
        super(appProperties, tracer);
    }
}
