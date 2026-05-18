package cn.hdfk7.boot.starter.common;

import cn.hdfk7.boot.starter.common.component.MybatisPlusComponent;
import cn.hdfk7.boot.starter.common.component.SnowflakeComponent;
import cn.hdfk7.boot.starter.common.component.ValidatorComponent;
import cn.hdfk7.boot.starter.common.component.XxlJobComponent;
import cn.hdfk7.boot.starter.common.properties.HttpHeaderProperties;
import cn.hdfk7.boot.starter.common.properties.MybatisPlusProperties;
import cn.hdfk7.boot.starter.common.properties.XxlJobProperties;
import cn.hdfk7.boot.starter.common.sentinel.FluxBlockExceptionHandler;
import cn.hdfk7.boot.starter.common.sentinel.MvcBlockExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = ValidationAutoConfiguration.class)
@EnableConfigurationProperties({
        HttpHeaderProperties.class,
        MybatisPlusProperties.class,
        XxlJobProperties.class
})
@Import({
        FluxBlockExceptionHandler.class,
        MvcBlockExceptionHandler.class,
        MybatisPlusComponent.class,
        SnowflakeComponent.class,
        ValidatorComponent.class,
        XxlJobComponent.class
})
public class BootStarterCommonAutoConfiguration {

}
