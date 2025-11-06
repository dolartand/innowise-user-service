package com.innowise.userservice.dto.user;

import com.innowise.userservice.dto.card.CardResponseDto;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CardResponseDto> cards
) {
}
