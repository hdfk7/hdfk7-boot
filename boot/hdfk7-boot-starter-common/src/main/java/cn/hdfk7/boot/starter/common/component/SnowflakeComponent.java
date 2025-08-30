package cn.hdfk7.boot.starter.common.component;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@ConditionalOnClass(value = {RedisOperations.class})
public class SnowflakeComponent {
    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动）
     */
    private final long twepoch = 1288834974657L;
    /**
     * 机器标识位数
     */
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    /**
     * 毫秒内自增位
     */
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /**
     * 时间戳左移动位
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);

    private long workerId;

    /**
     * 数据标识 ID 部分
     */
    private long datacenterId;
    /**
     * 并发控制
     */
    private long sequence = 0L;
    /**
     * 上次生产 ID 时间戳
     */
    private long lastTimestamp = -1L;

    @PostConstruct
    public void init() {
        this.datacenterId = getDatacenterId();
        this.workerId = getMaxWorkerId();
    }


    /**
     * 获取 maxWorkerId
     */
    protected long getMaxWorkerId() {
        Long dcId = stringRedisTemplate.opsForValue().increment("sequence:" + appName + ":workerId");
        log.debug("workerId:{}", dcId);
        if (ObjUtil.isNull(dcId) || dcId >= Integer.MAX_VALUE) {
            dcId = 1L;
            stringRedisTemplate.opsForValue().set("sequence:" + appName + ":workerId", "1");
        }
        return dcId % (maxWorkerId + 1);
    }

    /**
     * 数据标识id部分
     */
    protected long getDatacenterId() {
        Long dcId = stringRedisTemplate.opsForValue().increment("sequence:" + appName + ":dataCenterId");
        log.debug("dcId:{}", dcId);
        if (ObjUtil.isNull(dcId) || dcId >= Integer.MAX_VALUE) {
            dcId = 1L;
            stringRedisTemplate.opsForValue().set("sequence:" + appName + ":dataCenterId", "1");
        }
        return dcId % (maxDatacenterId + 1);
    }

    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 获取下一个 ID
     *
     * @return 下一个 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        //闰秒
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 3 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return SystemClock.now();
    }
}
