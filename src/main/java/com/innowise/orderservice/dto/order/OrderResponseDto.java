package com.innowise.orderservice.dto.order;

import com.innowise.orderservice.client.dto.UserInfoDto;
import com.innowise.orderservice.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResponseDto (
        Long id,
        Long userId,
        UserInfoDto user,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderItemResponseDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
