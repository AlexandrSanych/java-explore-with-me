package ru.practicum.main.mapper;

import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.response.UserResponse;
import ru.practicum.main.dto.response.UserShortDto;
import ru.practicum.main.model.User;

public class UserMapper {
    public static User toUser(NewUserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}