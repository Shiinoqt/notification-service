package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for sending payment confirmation emails and uploading
 * the generated receipt PDF to S3.
 *
 * <p>The email is sent first. The S3 upload is then triggered asynchronously
 * using the same generated PDF bytes to avoid regenerating the receipt.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService {

    private final JavaMailSender mailSender;
    private final JasperReportService jasperReportService;
    private final ReceiptUploadService receiptUploadService;

    @Value("${app.mail.from}")
    private String fromEmail;

    /**
     * Sends a payment confirmation email with a PDF receipt attachment.
     *
     * <p>If the email is sent successfully, the same receipt PDF is uploaded
     * asynchronously to S3. Validation failures are treated as programming or
     * payload errors and result in {@link IllegalArgumentException}. SMTP and PDF
     * generation failures are logged clearly.</p>
     *
     * @param paymentResponse the payment information used to build the email and receipt
     * @param userId          the user identifier used to build the S3 receipt path
     * @throws IllegalArgumentException if the payload is invalid
     */
    public void sendOrderPaidEmail(PaymentResponse paymentResponse, UUID userId) {
        validateInputs(paymentResponse, userId);

        EmailContext context = buildEmailContext(paymentResponse, userId);

        log.info(
                "Starting payment email flow: orderId={}, transactionId={}, recipient={}, status={}",
                context.orderId(),
                context.transactionId(),
                context.recipient(),
                paymentResponse.getStatus()
        );

        byte[] pdfBytes = generateReceiptPdf(paymentResponse, context);
        MimeMessage message = buildMimeMessage(paymentResponse, context, pdfBytes);

        try {
            sendEmail(message, context);
            uploadReceiptAsync(paymentResponse, context, pdfBytes);
        } catch (MailException ex) {
            log.error(
                    "SMTP send failed: orderId={}, transactionId={}, recipient={}",
                    context.orderId(),
                    context.transactionId(),
                    context.recipient(),
                    ex
            );
        }
    }

    /**
     * Validates method inputs before starting the notification flow.
     *
     * @param paymentResponse the payment payload
     * @param userId          the user identifier
     */
    private void validateInputs(PaymentResponse paymentResponse, UUID userId) {
        if (paymentResponse == null) {
            throw new IllegalArgumentException("PaymentResponse cannot be null");
        }

        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        String email = paymentResponse.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing");
        }
    }

    /**
     * Builds a compact immutable context object used across the email flow.
     *
     * @param paymentResponse the payment payload
     * @param userId          the user identifier
     * @return a populated email context
     */
    private EmailContext buildEmailContext(PaymentResponse paymentResponse, UUID userId) {
        return new EmailContext(
                paymentResponse.getOrderId(),
                paymentResponse.getTransactionId(),
                paymentResponse.getEmail().trim(),
                userId.toString()
        );
    }

    /**
     * Generates the PDF receipt using JasperReports.
     *
     * @param paymentResponse the payment payload
     * @param context         the current email context
     * @return the generated PDF bytes
     */
    private byte[] generateReceiptPdf(PaymentResponse paymentResponse, EmailContext context) {
        try {
            log.debug(
                    "Generating PDF receipt: orderId={}, transactionId={}",
                    context.orderId(),
                    context.transactionId()
            );

            byte[] pdfBytes = jasperReportService.generatePaymentReceiptPdf(paymentResponse);

            log.info(
                    "PDF receipt generated: orderId={}, transactionId={}, pdfSizeBytes={}",
                    context.orderId(),
                    context.transactionId(),
                    pdfBytes.length
            );

            return pdfBytes;
        } catch (JRException ex) {
            log.error(
                    "PDF generation failed: orderId={}, transactionId={}, recipient={}",
                    context.orderId(),
                    context.transactionId(),
                    context.recipient(),
                    ex
            );
            throw new RuntimeException(
                    "Failed to generate receipt PDF for order " + context.orderId(),
                    ex
            );
        }
    }

    /**
     * Builds the MIME email message with the PDF attachment.
     *
     * @param paymentResponse the payment payload
     * @param context         the current email context
     * @param pdfBytes        the receipt PDF bytes
     * @return the prepared MIME message
     */
    private MimeMessage buildMimeMessage(
            PaymentResponse paymentResponse,
            EmailContext context,
            byte[] pdfBytes
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(context.recipient());
            helper.setSubject("Payment Successful");
            helper.setText(buildEmailBody(paymentResponse), false);
            helper.addAttachment(
                    "payment_receipt_" + context.orderId() + ".pdf",
                    new ByteArrayResource(pdfBytes)
            );

            return message;
        } catch (MessagingException ex) {
            log.error(
                    "Failed to build MIME message: orderId={}, transactionId={}, recipient={}",
                    context.orderId(),
                    context.transactionId(),
                    context.recipient(),
                    ex
            );
            throw new RuntimeException(
                    "Failed to build email for " + context.recipient(),
                    ex
            );
        }
    }

    /**
     * Sends the email through the configured SMTP transport.
     *
     * @param message the prepared email message
     * @param context the current email context
     */
    private void sendEmail(MimeMessage message, EmailContext context) {
        log.info(
                "Sending email via SMTP: orderId={}, transactionId={}, recipient={}",
                context.orderId(),
                context.transactionId(),
                context.recipient()
        );

        mailSender.send(message);

        log.info(
                "Email sent successfully: orderId={}, transactionId={}, recipient={}",
                context.orderId(),
                context.transactionId(),
                context.recipient()
        );
    }

    /**
     * Uploads the generated receipt PDF to S3 asynchronously.
     *
     * @param paymentResponse the payment payload
     * @param context         the current email context
     * @param pdfBytes        the receipt PDF bytes
     */
    private void uploadReceiptAsync(
            PaymentResponse paymentResponse,
            EmailContext context,
            byte[] pdfBytes
    ) {
        receiptUploadService
                .uploadReceipt(context.userId(), paymentResponse, pdfBytes)
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.warn(
                                "S3 receipt upload failed after email send: orderId={}, transactionId={}, error={}",
                                context.orderId(),
                                context.transactionId(),
                                ex.getMessage()
                        );
                        return;
                    }

                    log.info(
                            "Receipt uploaded to S3: orderId={}, transactionId={}, eTag={}",
                            context.orderId(),
                            context.transactionId(),
                            response.eTag()
                    );
                });
    }

    /**
     * Builds the plain-text email body sent to the customer.
     *
     * @param paymentResponse the payment payload
     * @return the email body content
     */
    private String buildEmailBody(PaymentResponse paymentResponse) {
        return """
                Hello,

                Your payment was successfully processed.

                Order ID: %s
                Transaction ID: %s
                Amount: %s
                Status: %s

                Thank you for your purchase.
                """.formatted(
                paymentResponse.getOrderId(),
                paymentResponse.getTransactionId(),
                paymentResponse.getAmount(),
                paymentResponse.getStatus()
        );
    }

    /**
     * Immutable context object holding values reused across the email flow.
     *
     * @param orderId       the order identifier
     * @param transactionId the transaction identifier
     * @param recipient     the recipient email address
     * @param userId        the user identifier as a string
     */
    private record EmailContext(
            Object orderId,
            Object transactionId,
            String recipient,
            String userId
    ) {
    }
}