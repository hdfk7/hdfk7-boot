package cn.hdfk7.boot.starter.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.http.header")
public class HttpHeaderProperties {
    private String tokenName = "token";
}
