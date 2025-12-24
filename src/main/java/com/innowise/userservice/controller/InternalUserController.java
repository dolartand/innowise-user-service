package com.innowise.userservice.controller;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Internal REST controller for inter-service communication only
 * Requires X-Service-Key
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE')")
public class InternalUserController {

    private final UserService userService;

    /**
     * Create new user (called in Auth Service during registration)
     * @param userRequestDto data for creating user
     * @return created user data
     */
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("Internal call: creating user with email: {}", userRequestDto.email());
        UserResponseDto createdUser = userService.saveUser(userRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    /**
     * Delete user (called in Auth Service for rollback during failed registration)
     * @param userId user id to delete
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("Internal call: deleting user with id: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user by email (called in Auth Service during login)
     * @param email user email
     * @return user data
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam String email) {
        log.info("Internal call: getting user by email: {}", email);
        UserResponseDto userResponseDto = userService.findUserByEmail(email);
        return ResponseEntity.ok(userResponseDto);
    }

    /**
     * Get user by ID (for inter-service calls)
     * Duplicates /api/v1/users/{id} but requires X-Service-Key
     * Used by Order Service
     * @param userId user id
     * @return user data
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long userId) {
        log.info("Internal call: getting user by id: {}", userId);
        UserResponseDto userResponseDto = userService.findUserById(userId);
        return ResponseEntity.ok(userResponseDto);
    }
}
