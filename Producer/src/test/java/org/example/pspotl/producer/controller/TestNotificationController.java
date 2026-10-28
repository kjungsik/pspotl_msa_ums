package org.example.pspotl.producer.controller;

import org.example.pspotl.producer.config.RabbitProducerConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestNotificationController {

    private final RabbitTemplate rabbitTemplate;

    // 1. 단건 테스트 발송
    // POST http://localhost:8080/api/v1/test/send-one?userId=USER_123
    @PostMapping("/send-one")
    public ResponseEntity<Map<String, Object>> sendOne(@RequestParam(defaultValue = "USER_123") String userId) {
        String messageId = UUID.randomUUID().toString();
        NotificationMessage message = new NotificationMessage(messageId, userId, "ALIMTALK", "테스트 알림 메시지입니다.");

        rabbitTemplate.convertAndSend(
                RabbitProducerConfig.EXCHANGE_NAME,
                RabbitProducerConfig.ROUTING_KEY,
                message
        );

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "messageId", messageId));
    }

    // 2. 대량 부하 테스트 발송 (1,000건 / 5,000건 테스트)
    // POST http://localhost:8080/api/v1/test/send-bulk?count=1000
    @PostMapping("/send-bulk")
    public ResponseEntity<Map<String, Object>> sendBulk(@RequestParam(defaultValue = "1000") int count) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            String userId = "USER_" + (i % 100);
            String messageId = UUID.randomUUID().toString();
            NotificationMessage message = new NotificationMessage(messageId, userId, "APP_PUSH", "대량 테스트 알림 " + i);

            rabbitTemplate.convertAndSend(
                    RabbitProducerConfig.EXCHANGE_NAME,
                    RabbitProducerConfig.ROUTING_KEY,
                    message
            );
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "requestedCount", count,
                "elapsedTimeMs", elapsedTime
        ));
    }

    @Data
    @NoArgsConstructor // Jackson 역직렬화를 위한 기본 생성자 추가
    @AllArgsConstructor
    public static class NotificationMessage {
        private String messageId;
        private String userId;
        private String channel;
        private String content;
    }
}