package cn.hdfk7.boot.starter.common.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusProperties {
    private DbType dialect = DbType.MYSQL;
}
