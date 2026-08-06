package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.listener.LoadBalancerEventListener;
import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {NacosServiceLookupAutoConfiguration.class, LoadBalancerCacheAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({NacosDiscoveryProperties.class, NacosServiceDiscovery.class, NacosServiceLookup.class})
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.cache.enabled", matchIfMissing = true)
public class LoadBalancerEventListenerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerEventListener loadBalancerEventListener(ApplicationContext applicationContext, NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup) {
        LoadBalancerCacheManager loadBalancerCacheManager = applicationContext.getBean(LoadBalancerCacheManager.class);
        return new LoadBalancerEventListener(loadBalancerCacheManager, nacosDiscoveryProperties, nacosServiceDiscovery, nacosServiceLookup);
    }
}
