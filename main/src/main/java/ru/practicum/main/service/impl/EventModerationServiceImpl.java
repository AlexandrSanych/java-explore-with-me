package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.ModerationRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.ModerationLogResponse;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.mapper.ModerationLogMapper;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.ModerationAction;
import ru.practicum.main.model.ModerationLog;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ModerationLogRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventModerationService;
import ru.practicum.main.service.EventRequestCountService;
import ru.practicum.main.service.EventStatsService;
import ru.practicum.main.service.EventValidationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventModerationServiceImpl implements EventModerationService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final EventValidationService validationService;
    private final EventStatsService statsService;
    private final EventRequestCountService requestCountService;

    private static final Long ADMIN_ID = 1L;

    @Override
    @Transactional
    public EventFullDto moderateEvent(Long eventId, ModerationRequest request) {
        Event event = getEventOrThrow(eventId);
        User moderator = getUserOrThrow(ADMIN_ID);

        ModerationAction action = parseAction(request.getAction());
        String comment = request.getComment();

        EventState newState = validationService.applyAdminStateAction(event, request.getAction(), comment);
        event.setState(newState);

        if (newState == EventState.PUBLISHED) {
            event.setPublishedOn(LocalDateTime.now());
            event.setModerationComment(null);
        }

        Event savedEvent = eventRepository.save(event);

        ModerationLog moderationLog = new ModerationLog();
        moderationLog.setEvent(event);
        moderationLog.setModerator(moderator);
        moderationLog.setAction(action);
        moderationLog.setComment(comment);
        moderationLog.setCreatedOn(LocalDateTime.now());
        moderationLogRepository.save(moderationLog);

        log.info("Событие {} промодерировано: {}, администратор: {}", eventId, action, ADMIN_ID);

        Long views = statsService.getViewsForEvent(eventId);
        Long confirmedRequests = requestCountService.getConfirmedRequestsCount(eventId);
        return EventMapper.toEventFullDto(savedEvent, views, confirmedRequests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationLogResponse> getModerationHistory(Long eventId) {
        getEventOrThrow(eventId);
        List<ModerationLog> logs = moderationLogRepository.findAllByEventIdOrderByCreatedOnDesc(eventId);
        return ModerationLogMapper.toModerationLogResponseList(logs);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private ModerationAction parseAction(String action) {
        if (action == null) {
            throw new BadRequestException("Действие не может быть пустым");
        }
        return switch (action) {
            case "PUBLISH_EVENT" -> ModerationAction.PUBLISHED;
            case "REJECT_EVENT" -> ModerationAction.REJECTED;
            case "SEND_TO_REWORK" -> ModerationAction.SENT_TO_REWORK;
            default -> throw new BadRequestException("Некорректное действие: " + action +
                    ". Доступные действия: PUBLISH_EVENT, REJECT_EVENT, SEND_TO_REWORK");
        };
    }
}