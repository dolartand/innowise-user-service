package com.innowise.userservice.dto.card;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CardResponseDto(
        Long id,
        String number,
        String holder,
        LocalDate expirationDate,
        Boolean active
) {
}
