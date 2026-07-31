package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.listener.LoadBalancerEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LoadBalancerEventListenerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerEventListener loadBalancerEventListener() {
        return new LoadBalancerEventListener();
    }
}
