package com.its.notificationservice.service;
import com.its.notificationservice.dto.PaymentResponse;

public interface EmailService {
    void sendOrderPaidEmail(PaymentResponse response);
}