package com.innowise.userservice.service;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDto findUserById(Long id);

    Page<UserResponseDto> findAllUsers(String name, String surname, Boolean active, Pageable pageable);

    UserResponseDto saveUser(UserRequestDto userRequestDto);

    UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);

    void deleteUser(Long id);

    void changeUserActivity(Long id, Boolean isActive);

    UserResponseDto findUserByEmail(String email);
}
