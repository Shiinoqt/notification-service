package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptUploadService {
//    private final JasperReportService jasperReportService;
    private final S3Service s3Service;

//    /**
//     * Generates the receipt PDF and uploads it to S3.
//     * Use this when no PDF has been produced yet (e.g. standalone upload flows).
//     */
//    public CompletableFuture<PutObjectResponse> generateAndUploadReceipt(String userId,
//                                                                         PaymentResponse paymentResponse) {
//        try {
//            byte[] pdfBytes = jasperReportService.generatePaymentReceiptPdf(paymentResponse);
//
//            return s3Service.uploadReceiptPdf(
//                    userId,
//                    String.valueOf(paymentResponse.getOrderId()),
//                    String.valueOf(paymentResponse.getTransactionId()),
//                    pdfBytes
//            );
//        } catch (JRException e) {
//            log.error("Failed to generate receipt PDF for upload: userId={}, orderId={}, error={}",
//                    userId, paymentResponse != null ? paymentResponse.getOrderId() : "unknown", e.getMessage(), e);
//            return CompletableFuture.failedFuture(e);
//        } catch (Exception e) {
//            log.error("Unexpected error generating or uploading receipt: userId={}, orderId={}, error={}",
//                    userId, paymentResponse != null ? paymentResponse.getOrderId() : "unknown", e.getMessage(), e);
//            return CompletableFuture.failedFuture(e);
//        }
//    }

    /**
     * Uploads a pre-generated PDF receipt to S3 without regenerating it.
     * Use this when the PDF has already been produced for another purpose
     * (e.g. as an email attachment) to avoid a redundant JasperReports compile+fill cycle.
     */
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

        return s3Service.uploadReceiptPdf(
                userId,
                String.valueOf(paymentResponse.getOrderId()),
                String.valueOf(paymentResponse.getTransactionId()),
                pdfBytes
        );
    }
}