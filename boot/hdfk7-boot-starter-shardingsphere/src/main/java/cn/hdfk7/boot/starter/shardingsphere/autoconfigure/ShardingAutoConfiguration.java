package cn.hdfk7.boot.starter.shardingsphere.autoconfigure;

import cn.hdfk7.boot.starter.shardingsphere.properties.ShardingSphereProperties;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@AutoConfiguration(before = {DataSourceAutoConfiguration.class})
@ConditionalOnClass(YamlShardingSphereDataSourceFactory.class)
@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "raw")
@EnableConfigurationProperties(ShardingSphereProperties.class)
public class ShardingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource shardingSphereDataSource(ShardingSphereProperties properties, Environment environment) throws SQLException, IOException {
        String raw = properties.getRaw();
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("spring.shardingsphere.raw must not be blank");
        }
        String resolvedConfig = environment.resolvePlaceholders(raw);
        return YamlShardingSphereDataSourceFactory.createDataSource(resolvedConfig.getBytes(StandardCharsets.UTF_8));
    }
}
