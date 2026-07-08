package ru.practicum.main.service;

import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;

public interface EventValidationService {

    void validateEventDateForCreate(LocalDateTime eventDate);

    void validateEventDateForUserUpdate(LocalDateTime eventDate);

    void validateEventDateForAdminUpdate(LocalDateTime eventDate);

    void validateDescription(String description);

    void validateUserCanUpdate(Event event);

    EventState applyUserStateAction(Event event, String stateAction);

    EventState applyAdminStateAction(Event event, String stateAction);
}