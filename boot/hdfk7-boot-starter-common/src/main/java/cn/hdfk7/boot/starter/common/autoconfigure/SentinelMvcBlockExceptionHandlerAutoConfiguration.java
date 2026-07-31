package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.sentinel.SentinelMvcBlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(value = {BlockExceptionHandler.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SentinelMvcBlockExceptionHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public SentinelMvcBlockExceptionHandler sentinelMvcBlockExceptionHandler() {
        return new SentinelMvcBlockExceptionHandler();
    }
}
