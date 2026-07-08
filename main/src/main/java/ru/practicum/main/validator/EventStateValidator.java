package ru.practicum.main.validator;

import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;

public class EventStateValidator {

    public static void validateUserUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }
    }

    public static void validateUserStateAction(Event event, String stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case "SEND_TO_REVIEW":
                if (event.getState() != EventState.CANCELED) {
                    throw new ConflictException("Событие можно отправить на модерацию только из статуса CANCELED");
                }
                event.setState(EventState.PENDING);
                break;
            case "CANCEL_REVIEW":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отменить только из статуса PENDING");
                }
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }

    public static void validateAdminStateAction(Event event, String stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно опубликовать только из статуса PENDING");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть не ранее чем через час от текущего момента");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case "REJECT_EVENT":
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }
}