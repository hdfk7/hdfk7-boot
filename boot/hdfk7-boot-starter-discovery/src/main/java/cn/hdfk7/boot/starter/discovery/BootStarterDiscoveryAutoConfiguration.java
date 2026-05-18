package cn.hdfk7.boot.starter.discovery;

import cn.hdfk7.boot.starter.discovery.component.RestTemplateComponent;
import cn.hdfk7.boot.starter.discovery.listener.GatewayLoadbalancerEventListener;
import cn.hdfk7.boot.starter.discovery.listener.LoadbalancerEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = LoadBalancerAutoConfiguration.class)
@Import({
        GatewayLoadbalancerEventListener.class,
        LoadbalancerEventListener.class,
        RestTemplateComponent.class
})
public class BootStarterDiscoveryAutoConfiguration {
}
