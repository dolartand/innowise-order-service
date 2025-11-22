package com.innowise.orderservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ValidationErrorDto> validationErrors
) {
    @Builder
    public record ValidationErrorDto(
            String field,
            String rejectedValue,
            String message
    ) {

    }
}
