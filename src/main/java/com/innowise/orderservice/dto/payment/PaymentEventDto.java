package com.innowise.orderservice.dto.payment;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentEventDto(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        LocalDateTime timestamp,
        String eventType
) {
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
