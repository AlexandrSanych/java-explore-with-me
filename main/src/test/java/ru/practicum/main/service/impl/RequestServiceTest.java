package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.Request;
import ru.practicum.main.model.RequestStatus;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void addParticipationRequest_shouldCreateRequest_whenEventPublished() {
        Long userId = 1L;
        Long eventId = 1L;

        User user = User.builder().id(userId).name("Test User").build();
        User initiator = User.builder().id(2L).name("Initiator").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .initiator(initiator)
                .requestModeration(true)
                .participantLimit(10)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(eventId, userId)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request req = invocation.getArgument(0);
            req.setId(1L);
            req.setEvent(event);
            req.setRequester(user);
            req.setStatus(RequestStatus.PENDING);
            req.setCreated(LocalDateTime.now());
            return req;
        });

        ParticipationRequestDto response = requestService.addParticipationRequest(userId, eventId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void addParticipationRequest_shouldThrowConflictException_whenInitiatorRequestsOwnEvent() {
        Long userId = 1L;

        User user = User.builder().id(userId).name("Test User").build();
        Event event = Event.builder()
                .id(1L)
                .initiator(user)
                .state(EventState.PUBLISHED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.addParticipationRequest(userId, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Инициатор события не может добавить запрос");
    }

    @Test
    void addParticipationRequest_shouldThrowConflictException_whenEventNotPublished() {
        Long userId = 1L;

        User user = User.builder().id(userId).name("Test User").build();
        User initiator = User.builder().id(2L).name("Initiator").build();
        Event event = Event.builder()
                .id(1L)
                .state(EventState.PENDING)
                .initiator(initiator)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.addParticipationRequest(userId, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Нельзя участвовать в неопубликованном событии");
    }

    @Test
    void cancelRequest_shouldCancelRequest_whenExists() {
        Long userId = 1L;
        Long requestId = 1L;

        User user = User.builder().id(userId).name("Test User").build();
        Event event = Event.builder().id(1L).build();

        Request request = Request.builder()
                .id(requestId)
                .requester(user)
                .event(event)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(requestRepository.findByIdAndRequesterId(requestId, userId))
                .thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        ParticipationRequestDto response = requestService.cancelRequest(userId, requestId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(requestId);
        assertThat(response.getStatus()).isEqualTo("CANCELED");
    }

    @Test
    void cancelRequest_shouldThrowNotFoundException_whenRequestNotExists() {
        Long userId = 1L;
        Long requestId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(requestRepository.findByIdAndRequesterId(requestId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(userId, requestId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Запрос с id " + requestId + " не найден");
    }
}