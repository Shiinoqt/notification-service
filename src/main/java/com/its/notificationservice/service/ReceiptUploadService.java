package com.its.notificationservice.service;

import com.its.notificationservice.config.RabbitMQConfig;
import com.its.notificationservice.dto.PaymentReceiptCreatedEvent;
import com.its.notificationservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptUploadService {

    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;

    public CompletableFuture<PutObjectResponse> uploadReceipt(String userId,
                                                              PaymentResponse paymentResponse,
                                                              byte[] pdfBytes) {
        if (paymentResponse == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("PaymentResponse cannot be null"));
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.error("uploadReceipt called with empty PDF bytes: userId={}, orderId={}",
                    userId, paymentResponse.getOrderId());
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("pdfBytes cannot be null or empty"));
        }

        String orderId      = String.valueOf(paymentResponse.getOrderId());
        String transactionId = String.valueOf(paymentResponse.getTransactionId());

        // compute key now, before the async boundary
        String storageKey     = s3Service.buildReceiptKey(userId, orderId, transactionId);
        String receiptFileName = "receipt-" + orderId + "-" + transactionId + ".pdf";

        return s3Service.uploadReceiptPdf(userId, orderId, transactionId, pdfBytes)
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("S3 upload failed: userId={}, orderId={}, error={}",
                                userId, orderId, ex.getMessage(), ex);
                        return;
                    }
                    log.info("S3 upload successful, publishing receipt event: orderId={}, key={}",
                            orderId, storageKey);
                    publishReceiptEvent(paymentResponse.getOrderId(), receiptFileName);
                });
    }

    private void publishReceiptEvent(Object rawOrderId, String receiptFileName) {
        try {
            UUID orderId = rawOrderId instanceof UUID uuid
                    ? uuid
                    : UUID.fromString(rawOrderId.toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_RECEIPT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_RECEIPT_ROUTING_KEY,
                    new PaymentReceiptCreatedEvent(orderId, receiptFileName)
            );
            log.info("PaymentReceiptCreatedEvent published: orderId={}", orderId);
        } catch (Exception ex) {
            log.error("Failed to publish receipt event: orderId={}, error={}",
                    rawOrderId, ex.getMessage(), ex);
        }
    }
}