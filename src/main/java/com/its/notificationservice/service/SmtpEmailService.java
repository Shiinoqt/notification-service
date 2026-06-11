package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        log.info("Creating order mail");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(paymentResponse.getEmail().trim());
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

        mailSender.send(message);
        log.info("Order mail sent");
    }
}