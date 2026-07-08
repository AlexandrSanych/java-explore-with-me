package ru.practicum.main.service;

import java.util.List;
import java.util.Map;

public interface EventStatsService {

    Map<String, Long> getEventViewsMap(List<Long> eventIds);

    Long getViewsForEvent(Long eventId);

    void saveHit(String uri);
}