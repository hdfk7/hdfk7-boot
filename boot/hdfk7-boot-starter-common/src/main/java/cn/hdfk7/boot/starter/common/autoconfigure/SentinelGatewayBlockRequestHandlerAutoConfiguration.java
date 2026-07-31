package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.sentinel.SentinelGatewayBlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(value = {BlockRequestHandler.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SentinelGatewayBlockRequestHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public SentinelGatewayBlockRequestHandler sentinelGatewayBlockRequestHandler() {
        return new SentinelGatewayBlockRequestHandler();
    }
}
