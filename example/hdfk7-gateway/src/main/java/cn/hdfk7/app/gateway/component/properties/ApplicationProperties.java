package cn.hdfk7.app.gateway.component.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    private String urlWhitelist;
}
