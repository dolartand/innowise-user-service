package com.innowise.userservice.dto.user;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserRequestDto(

    @NotBlank(message = "Name is required")
    @Size(min = 3, message = "Name must be at least 3 characters long")
    String name,

    @NotBlank(message = "Surname is required")
    @Size(min = 3, message = "Surname must be at least 3 characters long")
    String surname,

    @NotBlank(message = "Birth date is required")
    @Past(message = "Birth date must be in past")
    LocalDate birthDate,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    @NotNull
    Boolean active
) {
}
