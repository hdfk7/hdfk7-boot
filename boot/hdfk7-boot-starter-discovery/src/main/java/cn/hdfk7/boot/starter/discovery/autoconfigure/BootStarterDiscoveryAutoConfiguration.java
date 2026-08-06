package cn.hdfk7.boot.starter.discovery.autoconfigure;

import cn.hdfk7.boot.starter.discovery.properties.AppProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;

@AutoConfiguration(before = {LoadBalancerAutoConfiguration.class})
@EnableConfigurationProperties({
        AppProperties.class
})
public class BootStarterDiscoveryAutoConfiguration {
}
