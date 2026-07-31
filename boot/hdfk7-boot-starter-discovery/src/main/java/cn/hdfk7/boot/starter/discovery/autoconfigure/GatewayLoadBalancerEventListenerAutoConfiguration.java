package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.listener.GatewayLoadBalancerEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(value = {RefreshRoutesEvent.class})
public class GatewayLoadBalancerEventListenerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public GatewayLoadBalancerEventListener gatewayLoadBalancerEventListener() {
        return new GatewayLoadBalancerEventListener();
    }
}
