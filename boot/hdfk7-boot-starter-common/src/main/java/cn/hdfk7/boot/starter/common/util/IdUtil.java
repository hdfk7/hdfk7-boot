package cn.hdfk7.boot.starter.common.util;

import cn.hdfk7.boot.starter.common.component.SnowflakeComponent;
import cn.hutool.extra.spring.SpringUtil;

public class IdUtil {
    public static String idStr() {
        return SpringUtil.getBean(SnowflakeComponent.class).nextIdStr();
    }

    public static long id() {
        return SpringUtil.getBean(SnowflakeComponent.class).nextId();
    }
}
