package ru.practicum.main.service;

import ru.practicum.main.dto.response.EventShortDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventSearchService {

    List<EventShortDto> searchEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            String sort,
            Integer from,
            Integer size
    );
}