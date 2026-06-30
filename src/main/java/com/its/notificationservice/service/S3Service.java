package com.its.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {
    private final S3AsyncClient s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public CompletableFuture<PutObjectResponse> uploadFileAsync(String targetBucketName, String key, Path filePath) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(targetBucketName)
                .key(key)
                .build();

        return s3Client.putObject(request, filePath)
                .whenComplete((response, exception) -> {
                    if (exception != null) {
                        log.error("Upload failed for key={}: {}", key, exception.getMessage(), exception);
                    } else {
                        log.info("File uploaded successfully: key={}, eTag={}", key, response.eTag());
                    }
                });
    }

    public CompletableFuture<PutObjectResponse> uploadReceiptPdf(
            String userId, String orderId, String transactionId, byte[] pdfBytes) {

        String key = buildReceiptKey(userId, orderId, transactionId);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .contentLength((long) pdfBytes.length)
                .build();

        return s3Client.putObject(request, AsyncRequestBody.fromBytes(pdfBytes));
    }

    public String buildReceiptKey(String userId, String orderId, String transactionId) {
        return String.format("p.rebong/%s/receipt-%s-%s.pdf", userId, orderId, transactionId);
    }
}