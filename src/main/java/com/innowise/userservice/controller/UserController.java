package com.innowise.userservice.controller;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing users
 * Path: /api/v1/users
 */

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Retrieves a user by id
     * @param id
     * @return user data
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id)  {
        UserResponseDto userResponseDto = userService.findUserById(id);
        return ResponseEntity.ok(userResponseDto);
    }

    /**
     * Getting a list of users with filtering and pagination
     * @param name filter by name
     * @param surname filter by surname
     * @param active filter by active
     * @param pageable pagination and sort params
     * @return page with users data
     */
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
            ) {
        Page<UserResponseDto> users = userService.findAllUsers(name, surname, active, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Create new user
     * @param userRequestDto data for creating user
     * @return created user data
     */
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        UserResponseDto createdUser = userService.saveUser(userRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    /**
     * Full user data update
     * @param id
     * @param userRequestDto new user data
     * @return updated user data
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto userRequestDto
    ) {
        UserResponseDto updatedUser = userService.updateUser(id, userRequestDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user
     * @param id
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change user activity status
     * @param id
     * @param isActive new activity status
     * @return 200 OK
     */
    @PatchMapping("/{id}/activity")
    public ResponseEntity<Void> changeUserActivity(
            @PathVariable Long id,
            @RequestParam Boolean isActive
    ) {
        userService.changeUserActivity(id, isActive);
        return ResponseEntity.ok().build();
    }
}
