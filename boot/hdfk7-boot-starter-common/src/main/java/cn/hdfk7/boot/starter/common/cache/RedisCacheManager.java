package cn.hdfk7.boot.starter.common.cache;

import cn.hutool.core.util.StrUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;

public class RedisCacheManager extends org.springframework.data.redis.cache.RedisCacheManager {

    public RedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @NotNull
    @Override
    protected RedisCache createRedisCache(@NotNull String name, RedisCacheConfiguration cacheConfiguration) {
        String[] array = StrUtil.splitToArray(name, "#");
        name = array[0];
        if (array.length > 1) {
            long ttl = Long.parseLong(array[1]);
            cacheConfiguration = cacheConfiguration.entryTtl(Duration.ofSeconds(ttl));
        }
        return super.createRedisCache(name, cacheConfiguration);
    }

}
