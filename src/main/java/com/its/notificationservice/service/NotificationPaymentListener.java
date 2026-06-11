package com.its.notificationservice.service;

import com.its.notificationservice.config.RabbitMQConfig;
import com.its.notificationservice.dto.PaymentResponse;
import com.its.notificationservice.model.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPaymentListener {
        private final EmailService emailService;

        @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
        public void handlePaymentResult(PaymentResponse response) {
                log.info("Received payment result: orderId={}, email={}, status={}", response.getOrderId(), response.getEmail(),response.getStatus());
                if (response.getStatus() == StatusEnum.ACCEPTED) {
                        emailService.sendOrderPaidEmail(response);
                }
                log.info("Order mail sent");
        }
}