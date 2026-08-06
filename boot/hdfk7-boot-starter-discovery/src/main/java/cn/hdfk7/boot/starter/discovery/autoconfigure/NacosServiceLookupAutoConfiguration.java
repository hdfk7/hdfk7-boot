package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceAutoConfiguration;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {NacosServiceAutoConfiguration.class, NacosDiscoveryAutoConfiguration.class})
@ConditionalOnClass(value = {NacosServiceManager.class, NacosDiscoveryProperties.class})
public class NacosServiceLookupAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public NacosServiceLookup nacosServiceLookup(NacosServiceManager nacosServiceManager, NacosDiscoveryProperties nacosDiscoveryProperties) {
        return new NacosServiceLookup(nacosServiceManager, nacosDiscoveryProperties);
    }
}
