package ru.practicum.main.mapper;

import ru.practicum.main.dto.request.NewEventRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EventMapper {
    public static Event toEvent(NewEventRequest request, User initiator, Category category) {
        return Event.builder()
                .annotation(request.getAnnotation())
                .category(category)
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .initiator(initiator)
                .location(LocationMapper.toLocation(request.getLocation()))
                .paid(request.getPaid() != null ? request.getPaid() : false)
                .participantLimit(request.getParticipantLimit() != null ? request.getParticipantLimit() : 0)
                .requestModeration(request.getRequestModeration() != null ? request.getRequestModeration() : true)
                .title(request.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static EventFullDto toEventFullDto(Event event, Long views, Long confirmedRequests) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryResponse(event.getCategory()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(LocationMapper.toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState() != null ? event.getState().name() : null)
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .moderationComment(event.getModerationComment())
                .reworkCount(event.getReworkCount() != null ? event.getReworkCount() : 0)
                .build();
    }

    public static EventShortDto toEventShortDto(Event event, Long views, Long confirmedRequests) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryResponse(event.getCategory()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .build();
    }

    public static List<EventShortDto> toEventShortDtoList(List<Event> events,
                                                          Map<Long, Long> viewsMap,
                                                          Map<Long, Long> confirmedMap) {
        if (events == null) return List.of();
        return events.stream()
                .map(event -> toEventShortDto(
                        event,
                        viewsMap != null ? viewsMap.getOrDefault(event.getId(), 0L) : 0L,
                        confirmedMap != null ? confirmedMap.getOrDefault(event.getId(), 0L) : 0L))
                .toList();
    }
}