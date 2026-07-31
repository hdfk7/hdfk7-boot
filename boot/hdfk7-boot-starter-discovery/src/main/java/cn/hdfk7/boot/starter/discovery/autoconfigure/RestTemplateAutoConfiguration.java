package cn.hdfk7.boot.starter.discovery.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@ConditionalOnClass(value = {RestTemplate.class})
public class RestTemplateAutoConfiguration {
    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(value = {RestTemplate.class}, name = {"loadBalancedRestTemplate"})
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
