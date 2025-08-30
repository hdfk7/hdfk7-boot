package cn.hdfk7.boot.starter.common.component;

import cn.hdfk7.boot.proto.base.json.JacksonInstance;
import cn.hdfk7.boot.starter.common.cache.RedisCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnClass(value = {CacheManager.class, RedisConnectionFactory.class, RedisCacheManager.class})
public class CacheComponent {

    @Bean
    @ConditionalOnMissingBean(value = {CacheManager.class})
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> name + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(JacksonInstance.getNonFinalMapper())))
                .entryTtl(Duration.ofDays(1));
        return new RedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultCacheConfig);
    }

}
