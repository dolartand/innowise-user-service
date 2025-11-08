package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.specification.UserSpecification;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponseDto findUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id" + id));
    }

    @Override
    @Cacheable(
            value = "users",
            key = "#name + '_' + #surname + '_' + #active + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()",
            unless = "#result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<UserResponseDto> findAllUsers(String name, String surname, Boolean active, Pageable pageable) {
        Specification<User> spec = UserSpecification.hasName(name)
                .and(UserSpecification.hasSurname(surname))
                .and(UserSpecification.isActive(active));
        return userRepository.findAll(spec, pageable)
                .map(userMapper::toUserResponseDto);
    }

    @Override
    @CachePut(value = "user", key = "#result.id()")
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public UserResponseDto saveUser(UserRequestDto userRequestDto) {
        if (userRepository.existsByEmail(userRequestDto.email())) {
            throw new BusinessException("User with email" + userRequestDto.email() + " already exists");
        }
        User userToSave = userMapper.toUser(userRequestDto);
        User savedUser = userRepository.save(userToSave);
        return userMapper.toUserResponseDto(savedUser);
    }

    @Override
    @CachePut(value = "user", key = "#id")
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id" + id));

        userRepository.findByEmail(userRequestDto.email()).ifPresent(user -> {
            if (!user.getId().equals(id)) {
                throw new BusinessException("Email " + userRequestDto.email() + " is already taken");
            }
        });

        userMapper.updateUserFromDto(userRequestDto, userToUpdate);
        User updatedUser = userRepository.save(userToUpdate);
        return userMapper.toUserResponseDto(updatedUser);
    }

    @Override
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @Transactional
    public void changeUserActivity(Long id, Boolean isActive) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id " + id);
        }

        if (isActive) {
            userRepository.activateUser(id);
        } else {
            userRepository.deactivateUser(id);
        }
    }
}
