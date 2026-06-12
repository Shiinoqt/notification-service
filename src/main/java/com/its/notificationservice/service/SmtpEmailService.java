package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendOrderPaidEmail(PaymentResponse paymentResponse) {
        if (paymentResponse == null) {
            throw new IllegalArgumentException("PaymentResponse cannot be null");
        }

        if (paymentResponse.getEmail() == null || paymentResponse.getEmail().isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing");
        }

        String recipient = paymentResponse.getEmail().trim();

        log.info("Creating order mail for {}", recipient);

        SimpleMailMessage message = getSimpleMailMessage(paymentResponse, recipient);

        mailSender.send(message);
        log.info("Order mail sent to {}", recipient);
    }

    private @NonNull SimpleMailMessage getSimpleMailMessage(PaymentResponse paymentResponse, String recipient) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipient);
        message.setSubject("Payment Successful");
        message.setText(
                "Hello,\n\n" +
                        "Your payment was successfully processed.\n\n" +
                        "Order ID: " + paymentResponse.getOrderId() + "\n" +
                        "Transaction ID: " + paymentResponse.getTransactionId() + "\n" +
                        "Amount: " + paymentResponse.getAmount() + "\n" +
                        "Status: " + paymentResponse.getStatus() + "\n\n" +
                        "Thank you for your purchase."
        );
        return message;
    }
}