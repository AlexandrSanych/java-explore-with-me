package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.response.UserResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void addUser_shouldCreateAndReturnUser() {
        NewUserRequest request = NewUserRequest.builder()
                .name("Test User")
                .email("test@test.com")
                .build();

        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@test.com")
                .build();

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.addUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void addUser_shouldThrowConflictException_whenEmailExists() {
        NewUserRequest request = NewUserRequest.builder()
                .name("Test User")
                .email("existing@test.com")
                .build();

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.addUser(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteUser_shouldDeleteUser_whenExists() {
        Long userId = 1L;
        User user = User.builder().id(userId).name("Test User").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUser_shouldThrowNotFoundException_whenNotExists() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(NotFoundException.class);
    }
}