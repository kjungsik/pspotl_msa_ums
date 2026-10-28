package org.example.pspotl.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqMsaConfig {

    public static final String MAIN_EXCHANGE = "notification.exchange";
    public static final String DLX_EXCHANGE = "notification.dlx";

    public static final String QUEUE_MARKETING = "notification.marketing.queue";
    public static final String QUEUE_DLQ = "notification.dlq";

    public static final String ROUTING_KEY_MARKETING = "notification.marketing";
    public static final String ROUTING_KEY_DLQ = "notification.dead-letter";

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue marketingQueue() {
        return QueueBuilder.durable(QUEUE_MARKETING)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding bindingMarketing() {
        return BindingBuilder.bind(marketingQueue()).to(mainExchange()).with(ROUTING_KEY_MARKETING);
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(ROUTING_KEY_DLQ);
    }

    // JSON 자동 직렬화/역직렬화 설정
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}