package com.innowise.orderservice.dto.item;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ItemResponseDto(
        Long id,
        String name,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
