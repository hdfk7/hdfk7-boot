package cn.hdfk7.boot.starter.discovery.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private List<String> urlWhitelist = List.of();
}
