package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.id.IdGenerator;
import cn.hdfk7.boot.starter.common.id.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {SnowflakeIdGeneratorAutoConfiguration.class})
@ConditionalOnBean(value = {SnowflakeIdGenerator.class})
public class IdGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return new IdGenerator(snowflakeIdGenerator);
    }
}
