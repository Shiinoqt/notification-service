package com.its.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Configuration
public class AwsS3ClientConfig {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access.key}")
    private String awsKey;

    @Value("${aws.secret.key}")
    private String awsSecret;

    @Bean
    public S3AsyncClient s3Client() {
        return S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsKey, awsSecret)
                        )
                )
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(2)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsKey, awsSecret)
                        )
                )
                .build();
    }
}