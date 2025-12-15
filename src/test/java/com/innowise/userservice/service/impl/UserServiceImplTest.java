package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit test")
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("findUserById tests")
    class  FindUserByIdTests {

        @Test
        @DisplayName("should successfully find user by id")
        void shouldFindUserById_Success() {
            Long userId = 1L;
            User user = createTestUser(userId);
            UserResponseDto expected = createTestUserResponseDto(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponseDto(user)).thenReturn(expected);

            UserResponseDto result =  userService.findUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            verify(userRepository, times(1)).findById(userId);
            verify(userMapper, times(1)).toUserResponseDto(user);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dont find user")
        void shouldThrowResourceNotFoundException_WhenUserNotFound() {
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("id " + userId);

            verify(userRepository, times(1)).findById(userId);
            verify(userMapper, never()).toUserResponseDto(any());
        }
    }

    @Nested
    @DisplayName("findAllUsers tests")
    class FindAllUsersTests {

        @Test
        @DisplayName("should return page of users with filters")
        void shouldFindAllUsers_WithFilters() {
            String name = "Ivan";
            String surname = "Ivanov";
            Boolean active = true;
            Pageable pageable = PageRequest.of(0, 10);

            User user1 = createTestUser(1L);
            User user2 = createTestUser(2L);
            List<User> users = List.of(user1, user2);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());

            UserResponseDto dto1 = createTestUserResponseDto(1L);
            UserResponseDto dto2 = createTestUserResponseDto(2L);

            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(userPage);
            when(userMapper.toUserResponseDto(user1)).thenReturn(dto1);
            when(userMapper.toUserResponseDto(user2)).thenReturn(dto2);

            Page<UserResponseDto> result = userService.findAllUsers(name, surname, active, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().getFirst().name()).isEqualTo("Ivan");

            verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(userMapper, times(2)).toUserResponseDto(any(User.class));

        }

        @Test
        @DisplayName("should return empty page when dont find users")
        void shouldReturnEmptyPage_WhenUserNotFound() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<UserResponseDto> result = userService.findAllUsers(null, null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(userMapper, never()).toUserResponseDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("saveUser tests")
    class SaveUserTests {

        @Test
        @DisplayName("should successfully create new user")
        void shouldSaveUser_Success() {
            UserRequestDto requestDto = createTestUserRequestDto();
            User userToSave = createTestUser(null);
            User savedUser = createTestUser(1L);
            UserResponseDto expected = createTestUserResponseDto(1L);

            when(userRepository.existsByEmail(requestDto.email())).thenReturn(false);
            when(userMapper.toUser(requestDto)).thenReturn(userToSave);
            when(userRepository.save(userToSave)).thenReturn(savedUser);
            when(userMapper.toUserResponseDto(savedUser)).thenReturn(expected);

            UserResponseDto result = userService.saveUser(requestDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo(requestDto.email());

            verify(userRepository, times(1)).existsByEmail(requestDto.email());
            verify(userMapper, times(1)).toUser(requestDto);
            verify(userRepository, times(1)).save(userToSave);
            verify(userMapper, times(1)).toUserResponseDto(savedUser);
        }

        @Test
        @DisplayName("should throw BusinessException when email exists")
        void shouldThrowBusinessException_WhenEmailExists() {
            UserRequestDto requestDto = createTestUserRequestDto();
            when(userRepository.existsByEmail(requestDto.email())).thenReturn(true);

            assertThatThrownBy(() -> userService.saveUser(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email")
                    .hasMessageContaining("already exists");

            verify(userRepository, times(1)).existsByEmail(requestDto.email());
            verify(userMapper, never()).toUser(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser tests")
    class UpdateUserTests {

        @Test
        @DisplayName("should successfully update existing user")
        void shouldUpdateUser_Success() {
            Long userId = 1L;
            UserRequestDto requestDto = createTestUserRequestDto();
            User existingUser = createTestUser(userId);
            User updatedUser = createTestUser(userId);
            UserResponseDto expected = createTestUserResponseDto(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(updatedUser);
            when(userMapper.toUserResponseDto(updatedUser)).thenReturn(expected);

            UserResponseDto result = userService.updateUser(userId, requestDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(userId);

            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).findByEmail(requestDto.email());
            verify(userMapper, times(1)).updateUserFromDto(requestDto, existingUser);
            verify(userRepository, times(1)).save(existingUser);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dont find user")
        void shouldThrowResourceNotFoundException_WhenUserNotFound() {
            Long userId = 999L;
            UserRequestDto requestDto = createTestUserRequestDto();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when email is taken by existing user")
        void shouldThrowBusinessException_WhenEmailIsTakenByExistingUser() {
            Long userId = 1L;
            Long existingUserId = 2L;
            UserRequestDto requestDto = createTestUserRequestDto();
            User existingUser = createTestUser(userId);
            User userWithSameEmail = createTestUser(existingUserId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.of(userWithSameEmail));

            assertThatThrownBy(() -> userService.updateUser(userId, requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email")
                    .hasMessageContaining("is already taken");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser tests")
    class DeleteUserTests {

        @Test
        @DisplayName("should successfully delete user")
        void shouldDeleteUser_Success() {
            Long userId = 1L;

            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUser(userId);

            verify(userRepository, times(1)).existsById(userId);
            verify(userRepository, times(1)).deleteById(userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user doesnt exists")
        void shouldThrowResourceNotFoundException_WhenUserDoesntExists() {
            Long userId = 999L;

            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, times(1)).existsById(userId);
            verify(userRepository, never()).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("changeUserActivity tests")
    class ChangeUserActivityTests {

        @Test
        @DisplayName("should activate user")
        void shouldActivateUser_Success() {
            Long userId = 1L;
            Boolean isActive = true;

            when(userRepository.existsById(userId)).thenReturn(true);
            when(userRepository.activateUser(userId)).thenReturn(1);

            userService.changeUserActivity(userId, isActive);

            verify(userRepository, times(1)).existsById(userId);
            verify(userRepository, times(1)).activateUser(userId);
            verify(userRepository, never()).deactivateUser(any());
        }

        @Test
        @DisplayName("should deactivate user")
        void shouldDeactivateUser_Success() {
            Long userId = 1L;
            Boolean isActive = false;

            when(userRepository.existsById(userId)).thenReturn(true);
            when(userRepository.deactivateUser(userId)).thenReturn(1);

            userService.changeUserActivity(userId, isActive);

            verify(userRepository, times(1)).existsById(userId);
            verify(userRepository, times(1)).deactivateUser(userId);
            verify(userRepository, never()).activateUser(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user doesnt exists")
        void shouldThrowResourceNotFoundException_WhenUserDoesntExists() {
            Long userId = 999L;

            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.changeUserActivity(userId, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, times(1)).existsById(userId);
            verify(userRepository, never()).activateUser(any());
            verify(userRepository, never()).deactivateUser(any());
        }
    }

    @Nested
    @DisplayName("findUserByEmail tests")
    class FindUserByEmailTests {

        @Test
        @DisplayName("should successfully find user by email")
        void shouldFindUserByEmail_Success() {
            String email = "ivan@example.com";
            Long userId = 1L;
            User user = createTestUser(userId);
            UserResponseDto expected = createTestUserResponseDto(userId);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponseDto(user)).thenReturn(expected);

            UserResponseDto result = userService.findUserByEmail(email);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            verify(userRepository, times(1)).findByEmail(email);
            verify(userMapper, times(1)).toUserResponseDto(user);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dont find user by email")
        void shouldThrowResourceNotFoundException_WhenUserNotFoundByEmail() {
            String email = "notfound@example.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserByEmail(email))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("email" + email);

            verify(userRepository, times(1)).findByEmail(email);
            verify(userMapper, never()).toUserResponseDto(any());
        }
    }


    private User createTestUser(Long userId) {
        return User.builder()
                .id(userId)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cards(new ArrayList<>())
                .build();
    }

    private UserRequestDto createTestUserRequestDto() {
        return UserRequestDto.builder()
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@example.com")
                .active(true)
                .build();
    }

    private UserResponseDto createTestUserResponseDto(Long id) {
        return UserResponseDto.builder()
                .id(id)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cards(List.of())
                .build();
    }
}
