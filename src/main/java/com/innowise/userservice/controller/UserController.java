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
import org.springframework.security.access.prepost.PreAuthorize;
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
     * Retrieves a user by id (user can get only itself)
     * @param id
     * @return user data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE') or #id == authentication.principal")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id)  {
        UserResponseDto userResponseDto = userService.findUserById(id);
        return ResponseEntity.ok(userResponseDto);
    }

    /**
     * Getting a list of users with filtering and pagination (only ADMIN)
     * @param name filter by name
     * @param surname filter by surname
     * @param active filter by active
     * @param pageable pagination and sort params
     * @return page with users data
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        UserResponseDto createdUser = userService.saveUser(userRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    /**
     * Full user data update (user can update only itself)
     * @param id
     * @param userRequestDto new user data
     * @return updated user data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto userRequestDto
    ) {
        UserResponseDto updatedUser = userService.updateUser(id, userRequestDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user (only ADMIN or SERVICE)
     * @param id
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change user activity status (only ADMIN)
     * @param id
     * @param isActive new activity status
     * @return 200 OK
     */
    @PatchMapping("/{id}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserActivity(
            @PathVariable Long id,
            @RequestParam Boolean isActive
    ) {
        userService.changeUserActivity(id, isActive);
        return ResponseEntity.ok().build();
    }

    /**
     * Get user by email (for Auth Service, requires ROLE_SERVICE)
     * @param email
     * @return user data
     */
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<UserResponseDto> getUserByEmail(@PathVariable String email) {
        UserResponseDto userResponseDto = userService.findUserByEmail(email);
        return ResponseEntity.ok(userResponseDto);
    }
}
