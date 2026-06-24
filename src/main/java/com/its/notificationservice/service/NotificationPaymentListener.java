package com.its.notificationservice.service;

import com.its.notificationservice.config.RabbitMQConfig;
import com.its.notificationservice.dto.PaymentResponse;
import com.its.notificationservice.model.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RabbitMQ listener responsible for handling payment notifications.
 *
 * <p>When an accepted payment event is received, this listener extracts the caller
 * identifier from the message headers and triggers the email notification flow.
 * Non-accepted payments are ignored.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPaymentListener {

        private static final String CALLER_HEADER = "caller";

        private final SmtpEmailService emailService;

        /**
         * Consumes payment result events from the notification queue.
         *
         * <p>Only accepted payments trigger an email notification. Invalid or incomplete
         * messages are logged and ignored to avoid endless retry loops for non-recoverable
         * cases such as missing headers.</p>
         *
         * @param response the payment result payload
         * @param message  the raw AMQP message containing headers
         */
        @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
        public void handlePaymentResult(PaymentResponse response, Message message) {
                if (response == null) {
                        log.warn("Received null payment response from notification queue");
                        return;
                }

                String userId = extractUserId(message.getMessageProperties());

                log.info(
                        "Received payment result: orderId={}, transactionId={}, email={}, status={}, userId={}",
                        response.getOrderId(),
                        response.getTransactionId(),
                        response.getEmail(),
                        response.getStatus(),
                        userId
                );

                if (response.getStatus() != StatusEnum.ACCEPTED) {
                        log.info(
                                "Skipping notification because payment is not accepted: orderId={}, transactionId={}, status={}",
                                response.getOrderId(),
                                response.getTransactionId(),
                                response.getStatus()
                        );
                        return;
                }

                if (userId == null || userId.isBlank()) {
                        log.error(
                                "Missing '{}' header for accepted payment: orderId={}, transactionId={}",
                                CALLER_HEADER,
                                response.getOrderId(),
                                response.getTransactionId()
                        );
                        return;
                }

                try {
                        emailService.sendOrderPaidEmail(response, UUID.fromString(userId));

                        log.info(
                                "Notification flow completed: orderId={}, transactionId={}, userId={}",
                                response.getOrderId(),
                                response.getTransactionId(),
                                userId
                        );
                } catch (IllegalArgumentException ex) {
                        log.error(
                                "Invalid userId in '{}' header: value={}, orderId={}, transactionId={}",
                                CALLER_HEADER,
                                userId,
                                response.getOrderId(),
                                response.getTransactionId(),
                                ex
                        );
                }
        }

        /**
         * Extracts the caller user identifier from message headers.
         *
         * @param properties the AMQP message properties
         * @return the header value as a string, or {@code null} if not present
         */
        private String extractUserId(MessageProperties properties) {
                if (properties == null || properties.getHeaders() == null) {
                        return null;
                }

                Object value = properties.getHeaders().get(CALLER_HEADER);
                return value != null ? value.toString() : null;
        }
}