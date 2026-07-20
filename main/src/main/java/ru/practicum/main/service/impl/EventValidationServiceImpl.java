package ru.practicum.main.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.service.EventValidationService;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EventValidationServiceImpl implements EventValidationService {

    @Override
    public void validateEventDateForCreate(LocalDateTime eventDate) {
        if (eventDate == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minDate = now.plusHours(2);
        if (eventDate.isBefore(minDate)) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }
    }

    @Override
    public void validateEventDateForUserUpdate(LocalDateTime eventDate) {
        if (eventDate == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (eventDate.isBefore(now.plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }
    }

    @Override
    public void validateEventDateForAdminUpdate(LocalDateTime eventDate) {
        if (eventDate == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (eventDate.isBefore(now.plusHours(1))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через час от текущего момента");
        }
    }

    @Override
    public void validateDescription(String description) {
        if (description != null) {
            if (description.length() < 20 || description.length() > 7000) {
                throw new BadRequestException("Длина описания должна быть от 20 до 7000 символов");
            }
        }
    }

    @Override
    public void validateUserCanUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }
    }

    @Override
    public EventState applyUserStateAction(Event event, String stateAction) {
        if (stateAction == null) {
            return event.getState();
        }

        switch (stateAction) {
            case "SEND_TO_REVIEW":
                if (event.getState() != EventState.CANCELED && event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отправить на модерацию " +
                            "только из статуса CANCELED или PENDING");
                }
                return EventState.PENDING;
            case "CANCEL_REVIEW":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отменить только из статуса PENDING");
                }
                return EventState.CANCELED;
            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }

    @Override
    public EventState applyAdminStateAction(Event event, String stateAction) {
        if (stateAction == null) {
            return event.getState();
        }

        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно опубликовать только из статуса PENDING");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть не ранее чем через час от текущего момента");
                }
                return EventState.PUBLISHED;
            case "REJECT_EVENT":
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
                return EventState.CANCELED;
            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }

    @Override
    public EventState applyAdminStateAction(Event event, String stateAction, String comment) {
        if (stateAction == null) {
            return event.getState();
        }

        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно опубликовать только из статуса PENDING");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть не ранее чем через час от текущего момента");
                }
                event.setModerationComment(null);
                return EventState.PUBLISHED;

            case "REJECT_EVENT":
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
                if (comment != null && !comment.isEmpty()) {
                    event.setModerationComment(comment);
                }
                return EventState.CANCELED;

            case "SEND_TO_REWORK":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отправить на доработку только из статуса PENDING");
                }
                if (comment == null || comment.isEmpty()) {
                    throw new BadRequestException("Необходимо указать комментарий для доработки");
                }
                event.setModerationComment(comment);
                Integer currentReworkCount = event.getReworkCount();
                if (currentReworkCount == null) {
                    currentReworkCount = 0;
                }
                event.setReworkCount(currentReworkCount + 1);
                return EventState.CANCELED;

            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }

    @Override
    public EventState applyUserStateActionWithRework(Event event, String stateAction) {
        if (stateAction == null) {
            return event.getState();
        }

        switch (stateAction) {
            case "SEND_TO_REVIEW":
                if (event.getState() != EventState.CANCELED && event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отправить на модерацию " +
                            "только из статуса CANCELED или PENDING");
                }
                event.setModerationComment(null);
                return EventState.PENDING;
            case "CANCEL_REVIEW":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно отменить только из статуса PENDING");
                }
                return EventState.CANCELED;
            default:
                throw new BadRequestException("Некорректное действие: " + stateAction);
        }
    }
}