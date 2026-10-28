package org.example.pspotl.consumer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConsumerConfig {

    public static final String QUEUE_NAME = "notification.marketing.queue";
    public static final String EXCHANGE_NAME = "notification.direct.exchange";
    public static final String ROUTING_KEY = "notification.marketing";

    // DLQ (Dead Letter Queue) 설정
    public static final String DLX_NAME = "notification.dlx";
    public static final String DLQ_NAME = "notification.dlq";
    public static final String DLQ_ROUTING_KEY = "notification.dlq.key";

    // 1. Queue 등록 (DLX 연동 설정)
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // 2. Main Direct Exchange
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // 3. Queue - Exchange Binding
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(ROUTING_KEY);
    }

    // --- DLQ (에러 발생 시 메시지 격리용) 설정 ---
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME, true, false);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    // 4. [핵심!] 브로커에 큐가 없을 경우 자동으로 Declare 해주는 RabbitAdmin
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    // 5. Producer가 전송한 JSON 메세지를 Java Object로 역직렬화
    // 5. 파라미터(ObjectMapper) 주입을 제거하고 직접 생성하여 전달
    @Bean
    public MessageConverter consumerMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 날짜/시간(LocalDateTime) 역직렬화 및 미알려진 필드 무시 설정
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}