package org.example.pspotl.consumer;

import lombok.extern.slf4j.Slf4j;
import org.example.pspotl.consumer.config.NotificationConsumer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

// Consumer 단
@Slf4j
@Component
public class NotificationConsumerListener {

    private final AtomicInteger count = new AtomicInteger(0);
    private long startTime = 0;

    @RabbitListener(queues = "notification.marketing.queue")
    public void receive(NotificationConsumer.NotificationMessage message) {
        if (count.incrementAndGet() == 1) {
            startTime = System.currentTimeMillis(); // 첫 메시지 수신 시각
        }

        // 실제 로직 처리...

        if (count.get() == 5000) {
            long totalConsumerTime = System.currentTimeMillis() - startTime;
            log.info(">>> Consumer 5,000건 처리 완료! 소요 시간: {} ms, Consumer TPS: {}",
                    totalConsumerTime, (5000.0 / totalConsumerTime) * 1000);
            count.set(0); // 카운터 초기화
        }
    }
}