package com.innowise.userservice.dto.card;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CardRequestDto(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "\\d{4}-\\d{4}-\\d{4}-\\d{4}",
                message = "Card number must be in the format XXXX-XXXX-XXXX-XXXX")
        String number,

        @NotBlank(message = "Holder is required")
        String holder,

        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in future")
        LocalDate expirationDate,

        @NotNull
        Boolean active
) {
}
