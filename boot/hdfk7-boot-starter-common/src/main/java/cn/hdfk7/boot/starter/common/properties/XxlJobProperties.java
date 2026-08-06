package cn.hdfk7.boot.starter.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {
    private boolean enabled;
    private String adminAddresses;
    private String accessToken;
    private int timeout;
    private String appName;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;
}
