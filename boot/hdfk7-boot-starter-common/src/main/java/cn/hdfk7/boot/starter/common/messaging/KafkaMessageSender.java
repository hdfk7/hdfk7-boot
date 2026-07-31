package cn.hdfk7.boot.starter.common.messaging;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class KafkaMessageSender {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, Object message) {
        this.send(topic, message, null);
    }

    public void send(String topic, Object message, BiConsumer<SendResult<String, String>, Throwable> callback) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, toMessage(message));
        if (callback != null) {
            future.whenComplete(callback);
        }
    }

    public SendResult<String, String> sendSync(String topic, Object message) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, toMessage(message));
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("send kafka message interrupted, topic=" + topic, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("send kafka message failed, topic=" + topic, e);
        }
    }

    private String toMessage(Object message) {
        return message instanceof String text ? text : JSONUtil.toJsonStr(message);
    }
}
