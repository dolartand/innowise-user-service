package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = CardMapper.class)
public interface UserMapper {

    User toUser(UserRequestDto userRequestDto);

    UserResponseDto toUserResponseDto(User user);

    void updateUserFromDto(UserRequestDto userRequestDto, @MappingTarget User user);
}
