package cn.hdfk7.boot.starter.common.autoconfigure;

import cn.hdfk7.boot.starter.common.messaging.KafkaMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration(after = {KafkaAutoConfiguration.class})
@ConditionalOnBean(value = {KafkaTemplate.class})
@ConditionalOnClass(value = {KafkaTemplate.class})
public class KafkaMessageSenderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public KafkaMessageSender kafkaMessageSender(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaMessageSender(kafkaTemplate);
    }
}
