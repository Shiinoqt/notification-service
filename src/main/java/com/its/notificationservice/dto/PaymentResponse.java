package com.its.notificationservice.dto;

import com.its.notificationservice.model.StatusEnum;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID orderId;
    private UUID transactionId;
    private String email;
    private BigDecimal amount;
    private StatusEnum status;
}
