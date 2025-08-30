package cn.hdfk7.boot.starter.common.component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnClass(value = {RestTemplate.class})
public class RestTemplateComponent {

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(value = {RestTemplate.class})
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
