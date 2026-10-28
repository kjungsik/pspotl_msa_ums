package org.example.pspotl.consumer.config;

import com.rabbitmq.client.Channel;
import org.example.pspotl.consumer.service.IdempotencyService;
import org.example.pspotl.consumer.service.ThrottlingService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final IdempotencyService idempotencyService;
    private final ThrottlingService throttlingService;

    @RabbitListener(queues = "notification.marketing.queue")
    public void consumeMessage(
            NotificationMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag
    ) throws IOException {

        String messageId = message.getMessageId();
        String channelType = message.getChannel(); // 예: APP_PUSH, ALIMTALK

        try {
            // STEP 1: Idempotency (중복 제거) 검사
            if (!idempotencyService.isFirstRequest(messageId)) {
                // 이미 처리된 메시지는 즉시 Ack 전송 후 종료 (유실/중복 발송 방지)
                channel.basicAck(tag, false);
                return;
            }

            // STEP 2: Rate Limiting (Throttling) 검사
            if (!throttlingService.isAllowed(channelType)) {
                // 초당 5,000건 초과 시: requeue=true로 Nack하여 잠시 후 다시 소비되도록 유도
                // (또는 지속 지연 시 DLQ로 전송 가능)
                channel.basicNack(tag, false, true);
                return;
            }

            // STEP 3: 실제 외부 알림 API 발송 모킹 (App Push / 알림톡)
            sendNotificationMock(message);

            // STEP 4: 성공 시 수동 Ack 전송
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[Consumer Error] 처리 중 예외 발생, DLQ로 우회: messageId={}", messageId, e);
            // 에러 발생 시 requeue=false 로 세팅하여 DLX(Dead Letter Exchange) 및 DLQ로 격리
            channel.basicNack(tag, false, false);
        }
    }

    private void sendNotificationMock(NotificationMessage message) {
        // External Push/SMS API Mocking 로직
        log.info("Received message: messageId={}, userId={}",
                message.getMessageId(), message.getUserId());
    }

    @Data
    public static class NotificationMessage {
        private String messageId;
        private String userId;
        private String channel;
        private String content;
    }
}