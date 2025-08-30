package cn.hdfk7.boot.starter.common.util;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

@Slf4j
public class RabbitUtil {

    private static Optional<RabbitTemplate> getRabbitTemplate() {
        try {
            return Optional.of(SpringUtil.getBean("rabbitTemplate"));
        } catch (Exception e) {
            log.warn("no configuration rabbitTemplate");
            return Optional.empty();
        }
    }

    public static void send(String exchange, String routingKey, Object message, MessagePostProcessor processor) {
        Optional<RabbitTemplate> rabbitTemplate = getRabbitTemplate();
        rabbitTemplate.ifPresent(_this -> _this.convertAndSend(exchange, routingKey, message instanceof String ? message : JSONUtil.toJsonStr(message), processor));
    }

    public static void send(String routingKey, Object message) {
        send("", routingKey, message, processor -> processor);
    }

    public static void send(String exchange, String routingKey, Object message) {
        send(exchange, routingKey, message, processor -> processor);
    }

    public static void send(String exchange, String routingKey, Object message, long delay) {
        send(exchange, routingKey, message, processor -> {
            processor.getMessageProperties().setDelayLong(delay);
            return processor;
        });
    }

    public static Object sendAndGet(String exchange, String routingKey, Object message, MessagePostProcessor processor) {
        Optional<RabbitTemplate> rabbitTemplate = getRabbitTemplate();
        return rabbitTemplate.map(_this -> _this.convertSendAndReceive(exchange, routingKey, message instanceof String ? message : JSONUtil.toJsonStr(message), processor)).orElse(null);
    }

    public static Object sendAndGet(String routingKey, Object message) {
        return sendAndGet("", routingKey, message, processor -> processor);
    }

    public static Object sendAndGet(String exchange, String routingKey, Object message) {
        return sendAndGet(exchange, routingKey, message, processor -> processor);
    }
}
