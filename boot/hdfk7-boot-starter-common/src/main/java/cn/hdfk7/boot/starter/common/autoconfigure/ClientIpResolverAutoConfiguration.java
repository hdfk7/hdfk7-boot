package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(value = {HttpServletRequest.class})
public class ClientIpResolverAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ClientIpResolver clientIpResolver() {
        return new ClientIpResolver();
    }
}
