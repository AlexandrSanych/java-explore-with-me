package ru.practicum.main.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.client.StatsClient;
import ru.practicum.main.dto.request.NewEventRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.LocationDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatsClient statsClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void addEvent_shouldCreateAndReturnEvent() {
        Long userId = 1L;
        Long categoryId = 1L;

        NewEventRequest request = NewEventRequest.builder()
                .annotation("Test annotation for event")
                .category(categoryId)
                .description("Test description for event")
                .eventDate(LocalDateTime.now().plusHours(3))
                .location(LocationDto.builder().lat(55.0f).lon(37.0f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .build();

        User user = User.builder().id(userId).name("Test User").build();
        Category category = Category.builder().id(categoryId).name("Test Category").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            return event;
        });

        EventFullDto response = eventService.addEvent(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAnnotation()).isEqualTo("Test annotation for event");
        assertThat(response.getTitle()).isEqualTo("Test Event");
        assertThat(response.getState()).isEqualTo("PENDING");
    }

    @Test
    void addEvent_shouldThrowBadRequestException_whenEventDateTooSoon() {
        Long userId = 1L;

        NewEventRequest request = NewEventRequest.builder()
                .annotation("Test annotation")
                .category(1L)
                .description("Test description")
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .title("Test Event")
                .build();

        User user = User.builder().id(userId).name("Test User").build();
        Category category = Category.builder().id(1L).name("Test Category").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> eventService.addEvent(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Дата события должна быть не ранее чем через 2 часа");
    }

    @Test
    void getPublicEvent_shouldReturnEvent_whenPublished() {
        Long eventId = 1L;
        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(1L).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PUBLISHED)
                .category(category)
                .initiator(user)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        when(eventRepository.findByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.of(event));

        EventFullDto response = eventService.getPublicEvent(eventId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(eventId);
        assertThat(response.getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getPublicEvent_shouldThrowNotFoundException_whenNotPublished() {
        Long eventId = 999L;
        when(eventRepository.findByIdAndState(eventId, EventState.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEvent(eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Опубликованное событие с id " + eventId + " не найдено");
    }
}