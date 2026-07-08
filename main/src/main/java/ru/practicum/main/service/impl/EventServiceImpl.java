package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.NewEventRequest;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.mapper.EventUpdateMapper;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventRequestCountService;
import ru.practicum.main.service.EventService;
import ru.practicum.main.service.EventStatsService;
import ru.practicum.main.service.EventValidationService;
import ru.practicum.main.service.PublicEventSearchService;
import ru.practicum.main.validator.PaginationValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final EventStatsService eventStatsService;
    private final EventRequestCountService eventRequestCountService;
    private final EventValidationService eventValidationService;
    private final PublicEventSearchService publicEventSearchService;

    // ===================== ПРИВАТНЫЕ МЕТОДЫ =====================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        PaginationValidator.validate(from, size);
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

        eventValidationService.validateEventDateForCreate(request.getEventDate());

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
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId +
                        " не найдено у пользователя " + userId));

        return enrichEventWithViewsAndRequests(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId +
                        " не найдено у пользователя " + userId));

        eventValidationService.validateUserCanUpdate(event);
        eventValidationService.validateEventDateForUserUpdate(request.getEventDate());
        eventValidationService.validateDescription(request.getDescription());

        Category category = request.getCategory() != null ? getCategoryOrThrow(request.getCategory()) : null;
        EventUpdateMapper.updateEventFromUserRequest(event, request, category);

        EventState newState = eventValidationService.applyUserStateAction(event, request.getStateAction());
        event.setState(newState);

        if (event.getState() == EventState.PENDING) {
            event.setPublishedOn(null);
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие пользователем {}: {}", userId, updatedEvent);

        return enrichEventWithViewsAndRequests(updatedEvent);
    }

    // ===================== ПУБЛИЧНЫЕ МЕТОДЫ =====================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {
        return publicEventSearchService.searchEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size
        );
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с id " + eventId + " не найдено"));

        saveHitAndIgnoreErrors("/events/" + eventId);

        return enrichEventWithViewsAndRequests(event);
    }

    private void saveHitAndIgnoreErrors(String uri) {
        try {
            eventStatsService.saveHit(uri);
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики для URI {}: {}", uri, e.getMessage(), e);
        }
    }

    // ===================== АДМИНСКИЕ МЕТОДЫ =====================

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {
        PaginationValidator.validate(from, size);

        int offset = from / size;
        int limit = size;

        List<Event> events = eventRepository.findEventsByAdminCriteria(
                users,
                states,
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

        eventValidationService.validateEventDateForAdminUpdate(request.getEventDate());
        eventValidationService.validateDescription(request.getDescription());

        Category category = request.getCategory() != null ? getCategoryOrThrow(request.getCategory()) : null;
        EventUpdateMapper.updateEventFromAdminRequest(event, request, category);

        EventState newState = eventValidationService.applyAdminStateAction(event, request.getStateAction());
        event.setState(newState);

        if (event.getState() == EventState.PUBLISHED) {
            event.setPublishedOn(LocalDateTime.now());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие администратором: {}", updatedEvent);

        return enrichEventWithViewsAndRequests(updatedEvent);
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

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

    private EventFullDto enrichEventWithViewsAndRequests(Event event) {
        Long views = eventStatsService.getViewsForEvent(event.getId());
        Long confirmedRequests = eventRequestCountService.getConfirmedRequestsCount(event.getId());

        return EventMapper.toEventFullDto(event, views, confirmedRequests);
    }

    private List<EventShortDto> enrichEventsWithViewsAndRequestsShort(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<String, Long> viewsMap = eventStatsService.getEventViewsMap(eventIds);
        Map<Long, Long> requestsMap = eventRequestCountService.getConfirmedRequestsMap(eventIds);

        return events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
                    Long confirmedRequests = requestsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, views, confirmedRequests);
                })
                .collect(Collectors.toList());
    }
}