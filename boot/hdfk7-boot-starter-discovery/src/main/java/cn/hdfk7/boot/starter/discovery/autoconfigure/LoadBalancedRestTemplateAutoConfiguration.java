package cn.hdfk7.boot.starter.discovery.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration(after = {RestTemplateAutoConfiguration.class})
@ConditionalOnClass(value = {RestTemplate.class, RestTemplateBuilder.class})
public class LoadBalancedRestTemplateAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(annotation = LoadBalanced.class)
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
