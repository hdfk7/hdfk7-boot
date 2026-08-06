package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.properties.MybatisPlusProperties;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(value = {MybatisPlusInterceptor.class, PaginationInnerInterceptor.class})
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(value = {MybatisPlusInterceptor.class})
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties mybatisPlusProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(mybatisPlusProperties.getDialect()));
        return interceptor;
    }
}
