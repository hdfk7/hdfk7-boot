package cn.hdfk7.boot.starter.common.id;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class SnowflakeIdGenerator {
    // 起始时间戳
    private static final long EPOCH = 1288834974657L;
    // 机器 ID 位数
    private static final long WORKER_BITS = 5L;
    // 数据中心 ID 位数
    private static final long DC_BITS = 5L;
    // 毫秒内序列位数
    private static final long SEQ_BITS = 12L;
    // 最大时钟回拨毫秒数
    private static final long CLOCK_BACK_MAX = 5L;

    private static final long MAX_REDIS_ID = Integer.MAX_VALUE;

    private final String app;
    private final StringRedisTemplate redis;

    // 机器 ID
    private final long workerId;
    // 数据中心 ID
    private final long dcId;
    // 毫秒内序列号
    private long sequence;
    // 上次生成 ID 的时间戳
    private long lastTime = -1L;

    public SnowflakeIdGenerator(@Value("${spring.application.name}") String app, StringRedisTemplate redis) {
        this.app = app;
        this.redis = redis;
        this.dcId = this.nextDcId();
        this.workerId = this.nextWorkerId();
    }

    protected long nextWorkerId() {
        Long id = redis.opsForValue().increment(redisKey("workerId"));
        log.debug("workerId:{}", id);
        if (ObjUtil.isNull(id) || id >= MAX_REDIS_ID) {
            id = 1L;
            redis.opsForValue().set(redisKey("workerId"), String.valueOf(id));
        }
        return id % (max(WORKER_BITS) + 1);
    }

    protected long nextDcId() {
        Long id = redis.opsForValue().increment(redisKey("dataCenterId"));
        log.debug("dcId:{}", id);
        if (ObjUtil.isNull(id) || id >= MAX_REDIS_ID) {
            id = 1L;
            redis.opsForValue().set(redisKey("dataCenterId"), String.valueOf(id));
        }
        return id % (max(DC_BITS) + 1);
    }

    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    public synchronized long nextId() {
        long time = now();
        if (time < lastTime) {
            long offset = lastTime - time;
            if (offset <= CLOCK_BACK_MAX) {
                try {
                    wait(offset << 1);
                    time = now();
                    if (time < lastTime) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTime == time) {
            sequence = (sequence + 1) & max(SEQ_BITS);
            if (sequence == 0) {
                time = nextMillis(lastTime);
            }
        } else {
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTime = time;

        return ((time - EPOCH) << (SEQ_BITS + WORKER_BITS + DC_BITS))
                | (dcId << (SEQ_BITS + WORKER_BITS))
                | (workerId << SEQ_BITS)
                | sequence;
    }

    protected long nextMillis(long lastTime) {
        long time = now();
        while (time <= lastTime) {
            time = now();
        }
        return time;
    }

    protected long now() {
        return SystemClock.now();
    }

    private long max(long bits) {
        return ~(-1L << bits);
    }

    private String redisKey(String name) {
        return "sequence:" + app + ":" + name;
    }
}
