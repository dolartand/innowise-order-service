package com.innowise.orderservice.dto.order;

import com.innowise.orderservice.dto.item.ItemResponseDto;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemResponseDto (
        Long id,
        ItemResponseDto item,
        Integer quantity,
        BigDecimal price,
        BigDecimal total
) {
}
