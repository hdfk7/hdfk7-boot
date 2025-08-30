package cn.hdfk7.boot.starter.common.util;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

@Slf4j
public class KafkaUtil {

    private static Optional<KafkaTemplate<String, String>> getKafkaTemplate() {
        try {
            return Optional.of(SpringUtil.getBean("kafkaTemplate"));
        } catch (Exception e) {
            log.warn("no configuration kafkaTemplate");
            return Optional.empty();
        }
    }

    public static void send(String channel, Object obj, BiConsumer<SendResult<String, String>, Throwable> callback) {
        Optional<KafkaTemplate<String, String>> kafkaTemplate = getKafkaTemplate();
        kafkaTemplate.ifPresent(_this -> {
            CompletableFuture<SendResult<String, String>> future = _this.send(channel, obj instanceof String ? (String) obj : JSONUtil.toJsonStr(obj));
            if (callback != null) {
                future.whenComplete(callback);
            }
        });
    }

    public static void send(String channel, Object obj) {
        send(channel, obj, null);
    }

    public static SendResult<String, String> sendSync(String channel, Object obj) {
        Optional<KafkaTemplate<String, String>> kafkaTemplate = getKafkaTemplate();
        if (kafkaTemplate.isPresent()) {
            try {
                CompletableFuture<SendResult<String, String>> future = kafkaTemplate.get().send(channel, obj instanceof String ? (String) obj : JSONUtil.toJsonStr(obj));
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
