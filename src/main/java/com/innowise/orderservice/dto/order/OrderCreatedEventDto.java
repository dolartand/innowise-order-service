package com.innowise.orderservice.dto.order;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderCreatedEventDto(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        String eventType
) {
}
