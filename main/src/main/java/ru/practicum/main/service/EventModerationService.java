package ru.practicum.main.service;

import ru.practicum.main.dto.request.ModerationRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.ModerationLogResponse;

import java.util.List;

public interface EventModerationService {
    EventFullDto moderateEvent(Long eventId, ModerationRequest request);
    List<ModerationLogResponse> getModerationHistory(Long eventId);
}