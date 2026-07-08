package ru.practicum.main.service;

import java.util.List;
import java.util.Map;

public interface EventRequestCountService {

    Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds);

    Long getConfirmedRequestsCount(Long eventId);
}