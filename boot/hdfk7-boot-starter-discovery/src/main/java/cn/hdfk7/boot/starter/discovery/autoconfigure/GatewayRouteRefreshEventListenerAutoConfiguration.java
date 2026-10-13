package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.listener.GatewayRouteRefreshEventListener;
import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {NacosServiceLookupAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(RefreshRoutesEvent.class)
@ConditionalOnBean({NacosDiscoveryProperties.class, NacosServiceDiscovery.class, NacosServiceLookup.class})
@ConditionalOnProperty(value = "spring.cloud.gateway.server.webflux.discovery.locator.enabled", havingValue = "true")
public class GatewayRouteRefreshEventListenerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public GatewayRouteRefreshEventListener gatewayRouteRefreshEventListener(NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup, ApplicationEventPublisher applicationEventPublisher) {
        return new GatewayRouteRefreshEventListener(nacosDiscoveryProperties, nacosServiceDiscovery, nacosServiceLookup, applicationEventPublisher);
    }
}