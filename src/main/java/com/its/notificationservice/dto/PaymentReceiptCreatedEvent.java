package com.its.notificationservice.dto;

import java.util.UUID;

public record PaymentReceiptCreatedEvent(
        UUID orderId,
        String receiptFileName
) {}