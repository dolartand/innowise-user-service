package com.innowise.userservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

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
