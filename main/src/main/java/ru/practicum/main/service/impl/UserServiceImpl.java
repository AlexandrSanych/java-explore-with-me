package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.response.UserResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.UserMapper;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.UserService;
import ru.practicum.main.validator.PaginationValidator;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse addUser(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с такой почтой уже существует");
        }

        User user = UserMapper.toUser(request);
        User savedUser = userRepository.save(user);
        log.info("Добавлен новый пользователь: {}", savedUser);
        return UserMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserOrThrow(userId);
        userRepository.delete(user);
        log.info("Удален пользователь с id: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(List<Long> ids, Integer from, Integer size) {
        PaginationValidator.validate(from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        if (ids != null && !ids.isEmpty()) {
            return userRepository.findAllByIdIn(ids, pageable)
                    .stream()
                    .map(UserMapper::toUserResponse)
                    .toList();
        }

        return userRepository.findAll(pageable)
                .stream()
                .map(UserMapper::toUserResponse)
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}