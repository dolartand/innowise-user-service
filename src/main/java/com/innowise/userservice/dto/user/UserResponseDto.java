package com.innowise.userservice.dto.user;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.innowise.userservice.dto.card.CardResponseDto;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
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
) implements Serializable {
}
