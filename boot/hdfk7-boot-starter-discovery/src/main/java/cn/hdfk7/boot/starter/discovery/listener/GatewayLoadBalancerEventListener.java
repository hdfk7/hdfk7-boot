package cn.hdfk7.boot.starter.discovery.listener;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.context.ApplicationEventPublisher;

public class GatewayLoadBalancerEventListener extends AbstractLoadBalancerEventListener {
    private final ApplicationEventPublisher applicationEventPublisher;

    public GatewayLoadBalancerEventListener(LoadBalancerCacheManager loadBalancerCacheManager, NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup, ApplicationEventPublisher applicationEventPublisher) {
        super(loadBalancerCacheManager, nacosDiscoveryProperties, nacosServiceDiscovery, nacosServiceLookup);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected void onNamingEvent(NamingEvent event) {
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(event));
    }
}
