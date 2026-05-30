package cn.hdfk7.boot.starter.common.messaging;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
@RequiredArgsConstructor
public class RabbitMessageSender {
    private static final String DEFAULT_DELAYED_EXCHANGE = "x-amq.delayed";

    private final RabbitTemplate rabbitTemplate;

    public void send(String routingKey, Object message) {
        this.send("", routingKey, message, null);
    }

    public void send(String routingKey, Object message, long delay) {
        this.send(DEFAULT_DELAYED_EXCHANGE, routingKey, message, msg -> {
            msg.getMessageProperties().setDelayLong(delay);
            return msg;
        });
    }

    public void send(String routingKey, Object message, int priority) {
        this.send("", routingKey, message, msg -> {
            msg.getMessageProperties().setPriority(priority);
            return msg;
        });
    }

    public void send(String exchange, String routingKey, Object message, MessagePostProcessor postProcessor) {
        Object payload = toMessage(message);
        if (postProcessor == null) {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, postProcessor);
        }
    }

    public Object sendSync(String routingKey, Object message) {
        return sendSync("", routingKey, message, null);
    }

    public Object sendSync(String routingKey, Object message, int priority) {
        return sendSync("", routingKey, message, msg -> {
            msg.getMessageProperties().setPriority(priority);
            return msg;
        });
    }

    public Object sendSync(String exchange, String routingKey, Object message, MessagePostProcessor postProcessor) {
        Object payload = toMessage(message);
        if (postProcessor == null) {
            return rabbitTemplate.convertSendAndReceive(exchange, routingKey, payload);
        }
        return rabbitTemplate.convertSendAndReceive(exchange, routingKey, payload, postProcessor);
    }

    private Object toMessage(Object message) {
        if (message instanceof String || message instanceof byte[]) {
            return message;
        }
        return JSONUtil.toJsonStr(message);
    }
}
