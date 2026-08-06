package cn.hdfk7.boot.starter.discovery.listener;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;

public class LoadBalancerEventListener extends AbstractLoadBalancerEventListener {
    public LoadBalancerEventListener(LoadBalancerCacheManager loadBalancerCacheManager, NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup) {
        super(loadBalancerCacheManager, nacosDiscoveryProperties, nacosServiceDiscovery, nacosServiceLookup);
    }
}
