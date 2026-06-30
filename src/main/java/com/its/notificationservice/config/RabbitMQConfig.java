package com.its.notificationservice.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for payment result notifications.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "queue-notification-payment";
    public static final String PAYMENT_RESULTS_EXCHANGE = "exchange-payment-results";
    public static final String PAYMENT_RESULTS_ROUTING_KEY = "payment.status.updated";
    public static final String PAYMENT_RECEIPT_EXCHANGE = "exchange-payment-receipts";
    public static final String PAYMENT_RECEIPT_ROUTING_KEY = "payment.receipt.created";

    @Bean
    public DirectExchange paymentReceiptExchange() {
        return new DirectExchange(PAYMENT_RECEIPT_EXCHANGE);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Queue that receives payment result events for notification processing.
     *
     * @return durable notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    /**
     * Exchange that publishes payment result events.
     *
     * @return payment results exchange
     */
    @Bean
    public DirectExchange paymentResultsExchange() {
        return new DirectExchange(PAYMENT_RESULTS_EXCHANGE);
    }

    /**
     * Binding between the payment results exchange and the notification queue.
     *
     * @return queue binding
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(paymentResultsExchange())
                .with(PAYMENT_RESULTS_ROUTING_KEY);
    }

    /**
     * Declares AMQP infrastructure on broker startup.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @return AMQP admin
     */
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Converts RabbitMQ JSON payloads to Java objects and back.
     *
     * @return Jackson-based message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Configures listener containers to deserialize JSON payloads for @RabbitListener methods.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @param jsonMessageConverter JSON message converter
     * @return listener container factory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}