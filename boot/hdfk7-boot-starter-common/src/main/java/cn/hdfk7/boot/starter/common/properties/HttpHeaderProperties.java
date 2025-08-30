package cn.hdfk7.boot.starter.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.http.header")
public class HttpHeaderProperties {
    private String tokenName = "token";
}
