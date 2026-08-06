package cn.hdfk7.boot.starter.common.id;

import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;
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

    private static final long NODE_BITS = WORKER_BITS + DC_BITS;
    private static final long NODE_COUNT = 1L << NODE_BITS;

    private final String applicationName;
    private final StringRedisTemplate redis;

    // 机器 ID
    private final long workerId;
    // 数据中心 ID
    private final long dcId;
    // 毫秒内序列号
    private long sequence;
    // 上次生成 ID 的时间戳
    private long lastTime = -1L;

    public SnowflakeIdGenerator(String applicationName, StringRedisTemplate redis) {
        this.applicationName = applicationName;
        this.redis = redis;
        long nodeId = nextNodeId();
        this.dcId = nodeId >> WORKER_BITS;
        this.workerId = nodeId & max(WORKER_BITS);
    }

    protected long nextNodeId() {
        Long sequence = redis.opsForValue().increment(redisKey());
        if (sequence == null) {
            throw new IllegalStateException("Failed to allocate snowflake node ID from Redis");
        }
        long nodeId = Math.floorMod(sequence - 1, NODE_COUNT);
        log.debug("snowflake nodeId:{}, sequence:{}", nodeId, sequence);
        return nodeId;
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for clock recovery", e);
                }
                time = now();
                if (time < lastTime) {
                    throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
                }
            } else {
                throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
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

    private String redisKey() {
        return "sequence:" + applicationName + ":" + "nodeId";
    }
}
