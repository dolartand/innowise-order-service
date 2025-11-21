package com.innowise.orderservice.dto.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ItemRequestDto (
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Item name should be between 2 and 255 characters")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price should be greater than 0")
        BigDecimal price
) {
}
