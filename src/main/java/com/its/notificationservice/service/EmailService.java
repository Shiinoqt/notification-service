package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface EmailService {
    void sendOrderPaidEmail(PaymentResponse response);
}
