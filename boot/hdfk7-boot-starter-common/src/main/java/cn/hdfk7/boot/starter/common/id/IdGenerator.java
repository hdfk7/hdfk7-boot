package cn.hdfk7.boot.starter.common.id;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdGenerator {
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public String idStr() {
        return snowflakeIdGenerator.nextIdStr();
    }

    public long id() {
        return snowflakeIdGenerator.nextId();
    }
}
