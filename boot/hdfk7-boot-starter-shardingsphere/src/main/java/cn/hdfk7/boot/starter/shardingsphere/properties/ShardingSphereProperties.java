package cn.hdfk7.boot.starter.shardingsphere.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.shardingsphere")
public class ShardingSphereProperties {
    private String raw;
}
