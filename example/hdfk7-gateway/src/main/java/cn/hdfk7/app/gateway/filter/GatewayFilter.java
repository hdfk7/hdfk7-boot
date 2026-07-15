package cn.hdfk7.app.gateway.filter;

import cn.hdfk7.boot.starter.discovery.filter.AbstractGatewayFilter;
import cn.hdfk7.boot.starter.discovery.properties.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class GatewayFilter extends AbstractGatewayFilter {
    public GatewayFilter(AppProperties appProperties) {
        super(appProperties);
    }
}
