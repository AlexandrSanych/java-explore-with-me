package ru.practicum.main.service;

import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse addUser(NewUserRequest request);

    void deleteUser(Long userId);

    List<UserResponse> getUsers(List<Long> ids, Integer from, Integer size);
}