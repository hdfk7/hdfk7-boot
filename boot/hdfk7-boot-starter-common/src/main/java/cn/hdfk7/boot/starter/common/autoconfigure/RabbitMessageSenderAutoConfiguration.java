package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.messaging.RabbitMessageSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {RabbitAutoConfiguration.class})
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMessageSenderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RabbitMessageSender rabbitMessageSender(RabbitTemplate rabbitTemplate) {
        return new RabbitMessageSender(rabbitTemplate);
    }
}
