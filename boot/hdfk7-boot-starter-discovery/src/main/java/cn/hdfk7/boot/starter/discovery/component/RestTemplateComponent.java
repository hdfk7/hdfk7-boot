package cn.hdfk7.boot.starter.discovery.component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(value = {RestTemplate.class})
public class RestTemplateComponent {

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(value = {RestTemplate.class}, name = {"loadBalancedRestTemplate"})
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

}
