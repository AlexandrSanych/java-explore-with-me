package ru.practicum.main.mapper;

import ru.practicum.main.dto.request.NewCompilationRequest;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationResponse;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;

import java.util.List;
import java.util.Map;

public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationRequest request, List<Event> events) {
        return Compilation.builder()
                .title(request.getTitle())
                .pinned(request.getPinned() != null ? request.getPinned() : false)
                .events(events != null ? events : List.of())
                .build();
    }

    public static CompilationResponse toCompilationResponse(Compilation compilation,
                                                            Map<String, Long> viewsMap,
                                                            Map<Long, Long> confirmedMap) {
        return CompilationResponse.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(event -> EventMapper.toEventShortDto(
                                event,
                                viewsMap.getOrDefault("/events/" + event.getId(), 0L),
                                confirmedMap.getOrDefault(event.getId(), 0L)))
                        .toList())
                .build();
    }

    public static void updateCompilationFromRequest(Compilation compilation,
                                                    UpdateCompilationRequest request,
                                                    List<Event> events) {
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            compilation.setEvents(events != null ? events : List.of());
        }
    }
}