package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.properties.DiscoveryScalarProperties;
import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientConfiguration;
import com.scalar.maven.webflux.SpringBootScalarProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration(after = {NacosServiceLookupAutoConfiguration.class, NacosDiscoveryClientConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBean(value = {DiscoveryClient.class, NacosServiceLookup.class})
@ConditionalOnClass(value = {SpringBootScalarProperties.class, RouteLocator.class})
@ConditionalOnProperty(prefix = "scalar", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "scalar.discovery", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "spring.application", name = "name")
public class ScalarAutoConfiguration {
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "scalar")
    public DiscoveryScalarProperties discoveryScalarProperties(DiscoveryClient discoveryClient, NacosServiceLookup nacosServiceLookup, @Value("${spring.application.name}") String applicationName, @Value("${springdoc.api-docs.path:/v3/api-docs}") String apiDocsPath) {
        return buildDiscoveryScalarProperties(discoveryClient, nacosServiceLookup, applicationName, apiDocsPath);
    }

    protected DiscoveryScalarProperties buildDiscoveryScalarProperties(DiscoveryClient discoveryClient, NacosServiceLookup nacosServiceLookup, String applicationName, String apiDocsPath) {
        return new DiscoveryScalarProperties(discoveryClient, nacosServiceLookup, applicationName, apiDocsPath);
    }
}
