package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = {DataRedisAutoConfiguration.class})
@ConditionalOnClass(value = {StringRedisTemplate.class})
@ConditionalOnBean(value = {StringRedisTemplate.class})
@ConditionalOnProperty(prefix = "spring.application", name = "name")
public class SnowflakeIdGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(StringRedisTemplate redis, @Value("${spring.application.name}") String applicationName) {
        return new SnowflakeIdGenerator(applicationName, redis);
    }
}
