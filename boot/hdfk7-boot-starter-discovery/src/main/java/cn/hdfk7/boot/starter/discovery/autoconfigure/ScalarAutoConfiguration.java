package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.properties.DiscoveryScalarProperties;
import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.scalar.maven.webflux.SpringBootScalarProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(value = {SpringBootScalarProperties.class, RouteLocator.class})
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "scalar.discovery", name = "enabled", havingValue = "true")
public class ScalarAutoConfiguration {
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "scalar")
    public SpringBootScalarProperties springBootScalarProperties(DiscoveryClient discoveryClient,
                                                                 NacosServiceLookup nacosServiceLookup,
                                                                 @Value("${spring.application.name:}") String applicationName,
                                                                 @Value("${scalar.excluded-services:}") List<String> excludedServices,
                                                                 @Value("${scalar.exclude-self:true}") boolean excludeSelf,
                                                                 @Value("${springdoc.api-docs.path:/v3/api-docs}") String apiDocsPath) {
        return discoveryScalarProperties(discoveryClient, nacosServiceLookup, applicationName, excludedServices, excludeSelf, apiDocsPath);
    }

    protected SpringBootScalarProperties discoveryScalarProperties(DiscoveryClient discoveryClient,
                                                                   NacosServiceLookup nacosServiceLookup,
                                                                   String applicationName,
                                                                   List<String> excludedServices,
                                                                   boolean excludeSelf,
                                                                   String apiDocsPath) {
        return new DiscoveryScalarProperties(discoveryClient, nacosServiceLookup, applicationName, excludedServices, excludeSelf, apiDocsPath);
    }
}
