package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.main.dto.request.NewCompilationRequest;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CompilationMapper;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.CompilationService;
import ru.practicum.main.validator.PaginationValidator;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public CompilationResponse addCompilation(NewCompilationRequest request) {
        if (compilationRepository.existsByTitle(request.getTitle())) {
            throw new ConflictException("Подборка с таким названием уже существует");
        }

        List<Event> events = getEventsByIds(request.getEvents());
        Compilation compilation = CompilationMapper.toCompilation(request, events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Добавлена новая подборка: {}", savedCompilation);
        return enrichSingleCompilation(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
        log.info("Удалена подборка с id: {}", compId);
    }

    @Override
    @Transactional
    public CompilationResponse updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationOrThrow(compId);

        if (request.getTitle() != null && !compilation.getTitle().equals(request.getTitle())) {
            if (compilationRepository.existsByTitle(request.getTitle())) {
                throw new ConflictException("Подборка с таким названием уже существует");
            }
        }

        List<Event> events = null;
        if (request.getEvents() != null) {
            events = getEventsByIds(request.getEvents());
        }

        CompilationMapper.updateCompilationFromRequest(compilation, request, events);
        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка: {}", updatedCompilation);
        return enrichSingleCompilation(updatedCompilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationResponse> getCompilations(Boolean pinned, Integer from, Integer size) {
        PaginationValidator.validate(from, size);

        List<Compilation> compilations;
        Pageable pageable = PageRequest.of(from / size, size);

        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return enrichCompilationsBatch(compilations);
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationResponse getCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        return enrichSingleCompilation(compilation);
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
    }

    private List<Event> getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            List<Long> foundIds = events.stream().map(Event::getId).toList();
            List<Long> notFound = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new NotFoundException("События с id " + notFound + " не найдены");
        }

        return events;
    }

    private List<CompilationResponse> enrichCompilationsBatch(List<Compilation> compilations) {
        if (compilations.isEmpty()) {
            return List.of();
        }

        List<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .distinct()
                .collect(Collectors.toList());

        if (allEventIds.isEmpty()) {
            return compilations.stream()
                    .map(c -> CompilationMapper.toCompilationResponse(c, Map.of(), Map.of()))
                    .collect(Collectors.toList());
        }

        Map<String, Long> viewsMap = getViewsMap(allEventIds);

        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(allEventIds);

        return compilations.stream()
                .map(compilation -> CompilationMapper.toCompilationResponse(
                        compilation,
                        viewsMap,
                        confirmedMap
                ))
                .collect(Collectors.toList());
    }

    private CompilationResponse enrichSingleCompilation(Compilation compilation) {
        List<Event> events = compilation.getEvents();
        if (events.isEmpty()) {
            return CompilationMapper.toCompilationResponse(compilation, Map.of(), Map.of());
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<String, Long> viewsMap = getViewsMap(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);

        return CompilationMapper.toCompilationResponse(compilation, viewsMap, confirmedMap);
    }

    private Map<String, Long> getViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(10),
                    LocalDateTime.now(),
                    uris,
                    true
            );

            return stats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка получения статистики для событий {}", eventIds, e);
            return new HashMap<>();
        }
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