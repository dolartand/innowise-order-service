package com.innowise.orderservice.dto.order;

import com.innowise.orderservice.enums.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderUpdateDto (
        OrderStatus status,

        @NotEmpty(message = "Order must contain at least 1 item")
        @Valid
        List<OrderItemRequestDto> items
) {
}
