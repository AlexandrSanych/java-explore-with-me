package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.EventRequestCountService;
import ru.practicum.main.service.EventStatsService;
import ru.practicum.main.service.PublicEventSearchService;
import ru.practicum.main.validator.PaginationValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicEventSearchServiceImpl implements PublicEventSearchService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventStatsService eventStatsService;
    private final EventRequestCountService eventRequestCountService;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            String sort,
            Integer from,
            Integer size) {

        PaginationValidator.validate(from, size);

        log.info("Поиск публичных событий с параметрами: text={}, categories={}, paid={}, " +
                        "rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        eventStatsService.saveHit("/events");

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        boolean sortByViews = "VIEWS".equalsIgnoreCase(sort);
        boolean filterAvailable = onlyAvailable != null && onlyAvailable;

        List<Event> allEvents = findAllEventsWithoutPagination(
                text, categories, paid, rangeStart, rangeEnd, filterAvailable
        );

        log.info("Найдено {} событий до сортировки и пагинации", allEvents.size());

        if (sortByViews) {
            List<Long> eventIds = allEvents.stream().map(Event::getId).collect(Collectors.toList());
            Map<String, Long> viewsMap = eventStatsService.getEventViewsMap(eventIds);

            allEvents.sort((e1, e2) -> {
                Long v1 = viewsMap.getOrDefault("/events/" + e1.getId(), 0L);
                Long v2 = viewsMap.getOrDefault("/events/" + e2.getId(), 0L);
                return v2.compareTo(v1);
            });
        }

        List<Event> paginatedEvents = allEvents.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

        log.info("После пагинации (from={}, size={}) осталось {} событий", from, size, paginatedEvents.size());

        return enrichEvents(paginatedEvents);
    }

    private List<Event> findAllEventsWithoutPagination(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable) {

        if (text != null && !text.trim().isEmpty()) {
            return eventRepository.findAllPublishedEventsWithText(
                    text.trim(),
                    categories,
                    paid,
                    rangeStart,
                    rangeEnd,
                    onlyAvailable
            );
        } else {
            return eventRepository.findAllPublishedEventsWithoutText(
                    categories,
                    paid,
                    rangeStart,
                    rangeEnd,
                    onlyAvailable
            );
        }
    }

    private List<EventShortDto> enrichEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);

        Map<String, Long> viewsMap = eventStatsService.getEventViewsMap(eventIds);

        return events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, views, confirmedRequests);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}