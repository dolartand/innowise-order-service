package com.innowise.orderservice.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderRequestDto (
        @NotNull(message = "User id is required")
        Long userId,

        @NotEmpty(message = "Order must contain at least 1 item")
        @Valid
        List<OrderItemRequestDto> items
) {
}
