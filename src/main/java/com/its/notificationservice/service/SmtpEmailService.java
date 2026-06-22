package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final JasperReportService jasperReportService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendOrderPaidEmail(PaymentResponse paymentResponse) {
        if (paymentResponse == null) {
            log.error("sendOrderPaidEmail called with null paymentResponse");
            throw new IllegalArgumentException("PaymentResponse cannot be null");
        }

        UUID orderId = paymentResponse.getOrderId();
        Object transactionId = paymentResponse.getTransactionId();
        String rawEmail = paymentResponse.getEmail();

        if (rawEmail == null || rawEmail.isBlank()) {
            log.error("Missing recipient email for orderId={}, transactionId={}", orderId, transactionId);
            throw new IllegalArgumentException("Recipient email is missing");
        }

        String recipient = rawEmail.trim();

        log.info(
                "Starting order paid email flow: orderId={}, transactionId={}, recipient={}, status={}",
                orderId, transactionId, recipient, paymentResponse.getStatus()
        );

        try {
            log.debug("Generating PDF receipt: orderId={}, transactionId={}", orderId, transactionId);
            byte[] pdfBytes = jasperReportService.generatePaymentReceiptPdf(paymentResponse);
            log.info(
                    "PDF receipt generated successfully: orderId={}, transactionId={}, pdfSizeBytes={}",
                    orderId, transactionId, pdfBytes.length
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.debug("Building MIME message: orderId={}, recipient={}", orderId, recipient);
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject("Payment Successful");
            helper.setText(buildEmailBody(paymentResponse), false);
            helper.addAttachment(
                    "payment_receipt_" + orderId + ".pdf",
                    new ByteArrayResource(pdfBytes)
            );

            log.info("Sending email via SMTP: orderId={}, transactionId={}, recipient={}", orderId, transactionId, recipient);
            mailSender.send(message);
            log.info("Email sent successfully: orderId={}, transactionId={}, recipient={}", orderId, transactionId, recipient);

        } catch (JRException e) {
            log.error(
                    "PDF generation failed during email flow: orderId={}, transactionId={}, recipient={}",
                    orderId, transactionId, recipient, e
            );
            throw new RuntimeException("Failed to generate receipt PDF for order " + orderId, e);

        } catch (MessagingException e) {
            log.error(
                    "Failed to build MIME email: orderId={}, transactionId={}, recipient={}",
                    orderId, transactionId, recipient, e
            );
            throw new RuntimeException("Failed to build email for " + recipient, e);

        } catch (Exception e) {
            log.error(
                    "Unexpected email send failure: orderId={}, transactionId={}, recipient={}",
                    orderId, transactionId, recipient, e
            );
            throw new RuntimeException("Failed to send email to " + recipient, e);
        }
    }

    private String buildEmailBody(PaymentResponse paymentResponse) {
        return "Hello,\n\n" +
                "Your payment was successfully processed.\n\n" +
                "Order ID: " + paymentResponse.getOrderId() + "\n" +
                "Transaction ID: " + paymentResponse.getTransactionId() + "\n" +
                "Amount: " + paymentResponse.getAmount() + "\n" +
                "Status: " + paymentResponse.getStatus() + "\n\n" +
                "Thank you for your purchase.";
    }
}