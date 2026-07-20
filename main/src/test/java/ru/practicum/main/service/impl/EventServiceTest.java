package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ModerationLogRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventRequestCountService;
import ru.practicum.main.service.EventStatsService;
import ru.practicum.main.service.EventValidationService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModerationLogRepository moderationLogRepository;

    @Mock
    private EventStatsService eventStatsService;

    @Mock
    private EventRequestCountService eventRequestCountService;

    @Mock
    private EventValidationService eventValidationService;

    @InjectMocks
    private EventServiceImpl eventService;

    // ===================== ТЕСТЫ ДЛЯ МОДЕРАЦИИ =====================

    @Test
    void updateEventByAdmin_shouldPublishEvent_whenStateActionIsPublish() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(1L).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyAdminStateAction(
                any(Event.class),
                eq("PUBLISH_EVENT"),
                isNull()
        )).thenReturn(EventState.PUBLISHED);

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByAdmin(eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("PUBLISHED");
    }

    @Test
    void updateEventByAdmin_shouldRejectEvent_whenStateActionIsReject() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("REJECT_EVENT")
                .moderationComment("Inappropriate content")
                .build();

        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(1L).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyAdminStateAction(
                any(Event.class),
                eq("REJECT_EVENT"),
                eq("Inappropriate content")
        )).thenReturn(EventState.CANCELED);

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByAdmin(eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("CANCELED");
    }

    @Test
    void updateEventByAdmin_shouldSendToRework_whenStateActionIsSendToRework() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("SEND_TO_REWORK")
                .moderationComment("Please add more details")
                .build();

        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(1L).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .reworkCount(0)
                .moderationComment(null)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyAdminStateAction(
                any(Event.class),
                eq("SEND_TO_REWORK"),
                eq("Please add more details")
        )).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            String comment = invocation.getArgument(2);
            e.setModerationComment(comment);
            e.setReworkCount(1);
            return EventState.CANCELED;
        });

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation
                -> invocation.getArgument(0));
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByAdmin(eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("CANCELED");
        assertThat(response.getModerationComment()).isEqualTo("Please add more details");
    }

    @Test
    void updateEventByAdmin_shouldThrowConflictException_whenTryingToPublishAlreadyPublishedEvent() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        doThrow(new ConflictException("Событие можно опубликовать только из статуса PENDING"))
                .when(eventValidationService).applyAdminStateAction(
                        any(Event.class),
                        eq("PUBLISH_EVENT"),
                        isNull()
                );

        assertThatThrownBy(() -> eventService.updateEventByAdmin(eventId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Событие можно опубликовать только из статуса PENDING");
    }

    @Test
    void updateEventByAdmin_shouldThrowBadRequestException_whenSendToReworkWithoutComment() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("SEND_TO_REWORK")
                .build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PENDING)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        doThrow(new BadRequestException("Необходимо указать комментарий для доработки"))
                .when(eventValidationService).applyAdminStateAction(
                        any(Event.class),
                        eq("SEND_TO_REWORK"),
                        isNull()
                );

        assertThatThrownBy(() -> eventService.updateEventByAdmin(eventId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Необходимо указать комментарий для доработки");
    }

    @Test
    void updateEventByAdmin_shouldClearModerationComment_whenEventIsPublished() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        Category category = Category.builder().id(1L).name("Test Category").build();
        User user = User.builder().id(1L).name("Test User").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .moderationComment("Old comment")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyAdminStateAction(
                any(Event.class),
                eq("PUBLISH_EVENT"),
                isNull()
        )).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setModerationComment(null);
            return EventState.PUBLISHED;
        });

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation
                -> invocation.getArgument(0));
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByAdmin(eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("PUBLISHED");
        assertThat(response.getModerationComment()).isNull();
    }

    @Test
    void updateEventByUser_shouldResubmitEvent_whenStateActionIsSendToReview() {
        Long userId = 1L;
        Long eventId = 1L;

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction("SEND_TO_REVIEW")
                .build();

        User user = User.builder().id(userId).name("Test User").build();
        Category category = Category.builder().id(1L).name("Test Category").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.CANCELED)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .moderationComment("Please fix")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyUserStateActionWithRework(
                any(Event.class),
                eq("SEND_TO_REVIEW")
        )).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setModerationComment(null);
            return EventState.PENDING;
        });

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation
                -> invocation.getArgument(0));
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByUser(userId, eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("PENDING");
        assertThat(response.getModerationComment()).isNull();
    }

    @Test
    void updateEventByUser_shouldCancelEvent_whenStateActionIsCancelReview() {
        Long userId = 1L;
        Long eventId = 1L;

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction("CANCEL_REVIEW")
                .build();

        User user = User.builder().id(userId).name("Test User").build();
        Category category = Category.builder().id(1L).name("Test Category").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test annotation")
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.now().plusHours(3))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        when(eventValidationService.applyUserStateActionWithRework(
                any(Event.class),
                eq("CANCEL_REVIEW")
        )).thenReturn(EventState.CANCELED);

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventStatsService.getViewsForEvent(any(Long.class))).thenReturn(0L);
        when(eventRequestCountService.getConfirmedRequestsCount(any(Long.class))).thenReturn(0L);

        EventFullDto response = eventService.updateEventByUser(userId, eventId, request);

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("CANCELED");
    }
}