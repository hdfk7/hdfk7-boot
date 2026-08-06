package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.listener.GatewayLoadBalancerEventListener;
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
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {NacosServiceLookupAutoConfiguration.class, LoadBalancerCacheAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(RefreshRoutesEvent.class)
@ConditionalOnBean({NacosDiscoveryProperties.class, NacosServiceDiscovery.class, NacosServiceLookup.class})
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.cache.enabled", matchIfMissing = true)
public class GatewayLoadBalancerEventListenerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public GatewayLoadBalancerEventListener gatewayLoadBalancerEventListener(ApplicationContext applicationContext, NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup, ApplicationEventPublisher applicationEventPublisher) {
        LoadBalancerCacheManager loadBalancerCacheManager = applicationContext.getBean(LoadBalancerCacheManager.class);
        return new GatewayLoadBalancerEventListener(loadBalancerCacheManager, nacosDiscoveryProperties, nacosServiceDiscovery, nacosServiceLookup, applicationEventPublisher);
    }
}
