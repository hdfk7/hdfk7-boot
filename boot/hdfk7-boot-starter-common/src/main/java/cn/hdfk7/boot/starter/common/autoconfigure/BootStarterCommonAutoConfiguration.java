package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.properties.HttpHeaderProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties({
        HttpHeaderProperties.class
})
public class BootStarterCommonAutoConfiguration {
}
