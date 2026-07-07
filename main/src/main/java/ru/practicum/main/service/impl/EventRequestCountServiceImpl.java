package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.main.model.RequestStatus;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.EventRequestCountService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRequestCountServiceImpl implements EventRequestCountService {

    private final RequestRepository requestRepository;

    @Override
    public Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        eventId -> requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                ));
    }

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }
}