package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.request.NewEventRequest;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.LocationDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventRequestCountService;
import ru.practicum.main.service.EventStatsService;
import ru.practicum.main.service.EventValidationService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    private EventStatsService eventStatsService;

    @Mock
    private EventRequestCountService eventRequestCountService;

    @Mock
    private EventValidationService eventValidationService;

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

        Event event = Event.builder()
                .id(1L)
                .annotation(request.getAnnotation())
                .category(category)
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .initiator(user)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title(request.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.addEvent(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAnnotation()).isEqualTo("Test annotation for event");
        assertThat(response.getTitle()).isEqualTo("Test Event");
        assertThat(response.getState()).isEqualTo("PENDING");
    }

    @Test
    void addEvent_shouldThrowConflictException_whenEventDateTooSoon() {
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

        doThrow(new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента"))
                .when(eventValidationService).validateEventDateForCreate(any(LocalDateTime.class));

        assertThatThrownBy(() -> eventService.addEvent(userId, request))
                .isInstanceOf(ConflictException.class)
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
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(10L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(5L);

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

    @Test
    void addEvent_shouldThrowConflictException_whenEventDateExactly2HoursAgo() {
        Long userId = 1L;

        NewEventRequest request = NewEventRequest.builder()
                .annotation("Test annotation")
                .category(1L)
                .description("Test description")
                .eventDate(LocalDateTime.now().plusHours(1).plusMinutes(59))
                .title("Test Event")
                .build();

        User user = User.builder().id(userId).name("Test User").build();
        Category category = Category.builder().id(1L).name("Test Category").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        doThrow(new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента"))
                .when(eventValidationService).validateEventDateForCreate(any(LocalDateTime.class));

        assertThatThrownBy(() -> eventService.addEvent(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Дата события должна быть не ранее чем через 2 часа");
    }

    @Test
    void updateEventByUser_shouldThrowConflictException_whenUserTriesToUpdatePublishedEvent() {
        Long userId = 1L;
        Long eventId = 1L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        doThrow(new ConflictException("Нельзя изменить опубликованное событие"))
                .when(eventValidationService).validateUserCanUpdate(any(Event.class));

        UpdateEventUserRequest request = UpdateEventUserRequest.builder().build();
        assertThatThrownBy(() -> eventService.updateEventByUser(userId, eventId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Нельзя изменить опубликованное событие");
    }

    @Test
    void updateEventByAdmin_shouldThrowConflictException_whenEventDateTooSoon() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .build();

        Event event = Event.builder()
                .id(eventId)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusHours(2))
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        doThrow(new ConflictException("Дата события должна быть не ранее чем через час от текущего момента"))
                .when(eventValidationService).validateEventDateForAdminUpdate(any(LocalDateTime.class));

        assertThatThrownBy(() -> eventService.updateEventByAdmin(eventId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Дата события должна быть не ранее чем через час");
    }

    @Test
    void getEventByUser_shouldReturnEvent_whenExists() {
        Long userId = 1L;
        Long eventId = 1L;
        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(userId).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.getEventByUser(userId, eventId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(eventId);
        assertThat(response.getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getEventByUser_shouldThrowNotFoundException_whenEventNotBelongToUser() {
        Long userId = 1L;
        Long eventId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventByUser(userId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Событие с id " + eventId + " не найдено у пользователя " + userId);
    }
}