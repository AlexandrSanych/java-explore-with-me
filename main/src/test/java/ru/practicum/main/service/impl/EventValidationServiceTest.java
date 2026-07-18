package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EventValidationServiceTest {

    private final EventValidationServiceImpl validationService = new EventValidationServiceImpl();

    // ===================== ТЕСТЫ ДЛЯ МОДЕРАЦИИ =====================

    @Test
    void applyAdminStateAction_shouldPublishEvent_whenStateIsPending() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusHours(2))
                .build();

        EventState result = validationService.applyAdminStateAction(event, "PUBLISH_EVENT", null);

        assertThat(result).isEqualTo(EventState.PUBLISHED);
        assertThat(event.getModerationComment()).isNull();
    }

    @Test
    void applyAdminStateAction_shouldThrowConflictException_whenPublishingNonPendingEvent() {
        Event event = Event.builder()
                .state(EventState.PUBLISHED)
                .build();

        assertThatThrownBy(() -> validationService.applyAdminStateAction(event, "PUBLISH_EVENT"
                , null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Событие можно опубликовать только из статуса PENDING");
    }

    @Test
    void applyAdminStateAction_shouldRejectEvent_whenStateIsPending() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .build();

        EventState result = validationService.applyAdminStateAction(event, "REJECT_EVENT"
                , "Inappropriate content");

        assertThat(result).isEqualTo(EventState.CANCELED);
        assertThat(event.getModerationComment()).isEqualTo("Inappropriate content");
    }

    @Test
    void applyAdminStateAction_shouldThrowConflictException_whenRejectingPublishedEvent() {
        Event event = Event.builder()
                .state(EventState.PUBLISHED)
                .build();

        assertThatThrownBy(() -> validationService.applyAdminStateAction(event, "REJECT_EVENT"
                , "Test"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Нельзя отклонить опубликованное событие");
    }

    @Test
    void applyAdminStateAction_shouldSendToRework_whenStateIsPendingAndCommentProvided() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .reworkCount(0)
                .build();

        EventState result = validationService.applyAdminStateAction(
                event, "SEND_TO_REWORK", "Please add more details");

        assertThat(result).isEqualTo(EventState.CANCELED);
        assertThat(event.getModerationComment()).isEqualTo("Please add more details");
        assertThat(event.getReworkCount()).isEqualTo(1);
    }

    @Test
    void applyAdminStateAction_shouldThrowBadRequestException_whenSendToReworkWithoutComment() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .build();

        assertThatThrownBy(() -> validationService.applyAdminStateAction(event, "SEND_TO_REWORK"
                , null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Необходимо указать комментарий для доработки");
    }

    @Test
    void applyAdminStateAction_shouldThrowConflictException_whenSendToReworkNonPendingEvent() {
        Event event = Event.builder()
                .state(EventState.PUBLISHED)
                .build();

        assertThatThrownBy(() -> validationService.applyAdminStateAction(event, "SEND_TO_REWORK"
                , "Test"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Событие можно отправить на доработку только из статуса PENDING");
    }

    @Test
    void applyUserStateActionWithRework_shouldSendToReview_whenStateIsCanceled() {
        Event event = Event.builder()
                .state(EventState.CANCELED)
                .moderationComment("Please fix")
                .build();

        EventState result = validationService.applyUserStateActionWithRework(event, "SEND_TO_REVIEW");

        assertThat(result).isEqualTo(EventState.PENDING);
        assertThat(event.getModerationComment()).isNull();
    }

    @Test
    void applyUserStateActionWithRework_shouldSendToReview_whenStateIsPending() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .moderationComment("Please fix")
                .build();

        EventState result = validationService.applyUserStateActionWithRework(event, "SEND_TO_REVIEW");

        assertThat(result).isEqualTo(EventState.PENDING);
        assertThat(event.getModerationComment()).isNull();
    }

    @Test
    void applyUserStateActionWithRework_shouldThrowConflictException_whenSendingPublishedEventToReview() {
        Event event = Event.builder()
                .state(EventState.PUBLISHED)
                .build();

        assertThatThrownBy(() -> validationService.applyUserStateActionWithRework(event, "SEND_TO_REVIEW"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Событие можно отправить на модерацию" +
                        " только из статуса CANCELED или PENDING");
    }

    @Test
    void applyUserStateActionWithRework_shouldCancelReview_whenStateIsPending() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .build();

        EventState result = validationService.applyUserStateActionWithRework(event, "CANCEL_REVIEW");

        assertThat(result).isEqualTo(EventState.CANCELED);
    }

    @Test
    void applyUserStateActionWithRework_shouldThrowConflictException_whenCancelingNonPendingEvent() {
        Event event = Event.builder()
                .state(EventState.CANCELED)
                .build();

        assertThatThrownBy(() -> validationService.applyUserStateActionWithRework(event, "CANCEL_REVIEW"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Событие можно отменить только из статуса PENDING");
    }

    @Test
    void applyUserStateActionWithRework_shouldThrowBadRequestException_whenInvalidAction() {
        Event event = Event.builder()
                .state(EventState.PENDING)
                .build();

        assertThatThrownBy(() -> validationService.applyUserStateActionWithRework(event, "INVALID_ACTION"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Некорректное действие");
    }
}