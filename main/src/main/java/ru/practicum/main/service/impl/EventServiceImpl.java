package ru.practicum.main.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.StatsClient;
import ru.practicum.main.dto.request.NewEventRequest;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.mapper.LocationMapper;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.RequestStatus;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventService;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final HttpServletRequest httpServletRequest;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        getUserOrThrow(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        return enrichEventsWithViewsAndRequestsShort(events);
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventRequest request) {
        User user = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(request.getCategory());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minDate = now.plusHours(2);
        if (request.getEventDate().isBefore(minDate)) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = EventMapper.toEvent(request, user, category);
        Event savedEvent = eventRepository.save(event);
        log.info("Добавлено новое событие: {}", savedEvent);
        return enrichEventWithViewsAndRequests(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено у пользователя "
                        + userId));
        return enrichEventWithViewsAndRequests(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено у пользователя "
                        + userId));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }

        if (request.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (request.getEventDate().isBefore(now.plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }
        if (request.getDescription() != null) {
            if (request.getDescription().length() < 20 || request.getDescription().length() > 7000) {
                throw new BadRequestException("Длина описания должна быть от 20 до 7000 символов");
            }
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(request.getLocation()));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
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
                    throw new BadRequestException("Некорректное действие: " + request.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие: {}", updatedEvent);
        return enrichEventWithViewsAndRequests(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {
        log.info("Получение публичных событий с параметрами: text={}, categories={}, paid={}, " +
                        "rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        saveHit("/events");

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        boolean sortByViews = "VIEWS".equalsIgnoreCase(sort);

        int offset = from / size;
        int limit = size;

        List<Event> events;
        if (text != null && !text.trim().isEmpty()) {
            events = eventRepository.findPublishedEventsByCriteria(
                    text.trim(),
                    categories,
                    paid,
                    rangeStart,
                    rangeEnd,
                    offset,
                    limit
            );
        } else {
            events = eventRepository.findPublishedEventsByCriteriaWithoutText(
                    categories,
                    paid,
                    rangeStart,
                    rangeEnd,
                    offset,
                    limit
            );
        }

        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(event -> {
                        Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(),
                                RequestStatus.CONFIRMED);
                        return event.getParticipantLimit() == 0 || confirmedRequests < event.getParticipantLimit();
                    })
                    .collect(Collectors.toList());
        }

        List<EventShortDto> enrichedEvents = enrichEventsWithViewsAndRequestsShort(events);

        if (sortByViews) {
            enrichedEvents.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
            return paginateList(enrichedEvents, from, size);
        }

        return enrichedEvents;
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с id " + eventId + " не найдено"));

        saveHit("/events/" + eventId);

        return enrichEventWithViewsAndRequests(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {
        List<String> eventStates = states;

        int offset = from / size;
        int limit = size;

        List<Event> events = eventRepository.findEventsByAdminCriteria(
                users,
                eventStates,
                categories,
                rangeStart,
                rangeEnd,
                offset,
                limit
        );

        return events.stream()
                .map(this::enrichEventWithViewsAndRequests)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventOrThrow(eventId);

        if (request.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (request.getEventDate().isBefore(now.plusHours(1))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через час от текущего момента");
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() < 20 || request.getDescription().length() > 7000) {
                throw new BadRequestException("Длина описания должна быть от 20 до 7000 символов");
            }
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(request.getLocation()));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно опубликовать только из статуса PENDING");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Дата события должна быть не ранее " +
                                "чем через час от текущего момента");
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
                    throw new BadRequestException("Некорректное действие: " + request.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие администратором: {}", updatedEvent);
        return enrichEventWithViewsAndRequests(updatedEvent);
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private void saveHit(String uri) {
        try {
            String clientIp = httpServletRequest.getRemoteAddr();
            if (clientIp == null || clientIp.isEmpty() ||
                    "0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hitDto);
            log.info("Сохранена статистика для URI: {}, IP: {}", uri, clientIp);
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage(), e);
        }
    }

    private Map<String, Long> getEventViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        try {
            LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            log.info("=== ЗАПРОС СТАТИСТИКИ: start={}, end={}, uris={} ===", start, end, uris);

            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            log.info("=== ПОЛУЧЕНА СТАТИСТИКА: {} ===", stats);

            return stats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка получения статистики просмотров: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        eventId -> requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                ));
    }

    private EventFullDto enrichEventWithViewsAndRequests(Event event) {
        Map<String, Long> viewsMap = getEventViewsMap(List.of(event.getId()));
        Map<Long, Long> requestsMap = getConfirmedRequestsMap(List.of(event.getId()));

        Long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
        Long confirmedRequests = requestsMap.getOrDefault(event.getId(), 0L);

        return EventMapper.toEventFullDto(event, views, confirmedRequests);
    }

    private List<EventShortDto> enrichEventsWithViewsAndRequestsShort(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<String, Long> viewsMap = getEventViewsMap(eventIds);
        Map<Long, Long> requestsMap = getConfirmedRequestsMap(eventIds);

        return events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
                    Long confirmedRequests = requestsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, views, confirmedRequests);
                })
                .collect(Collectors.toList());
    }

    private <T> List<T> paginateList(List<T> list, Integer from, Integer size) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        int start = Math.min(from, list.size());
        int end = Math.min(start + size, list.size());
        return list.subList(start, end);
    }
}