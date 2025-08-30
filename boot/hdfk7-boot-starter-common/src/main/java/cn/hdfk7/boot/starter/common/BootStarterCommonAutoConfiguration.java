package cn.hdfk7.boot.starter.common;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

@ComponentScan
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class BootStarterCommonAutoConfiguration {

}
