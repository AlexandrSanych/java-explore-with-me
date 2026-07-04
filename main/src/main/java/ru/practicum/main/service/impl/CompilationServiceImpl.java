package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.NewCompilationRequest;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CompilationMapper;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.RequestStatus;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.CompilationService;
import ru.practicum.main.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        return enrichCompilation(savedCompilation);
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

        List<Event> events = getEventsByIds(request.getEvents());
        CompilationMapper.updateCompilationFromRequest(compilation, request, events);
        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка: {}", updatedCompilation);
        return enrichCompilation(updatedCompilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationResponse> getCompilations(Boolean pinned, Integer from, Integer size) {
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned);
        } else {
            compilations = compilationRepository.findAll(PageRequest.of(from / size, size)).getContent();
        }
        return compilations.stream()
                .map(this::enrichCompilation)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationResponse getCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        return enrichCompilation(compilation);
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
    }

    private List<Event> getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findAllById(eventIds);
    }

    private CompilationResponse enrichCompilation(Compilation compilation) {
        List<Event> events = compilation.getEvents();
        if (events.isEmpty()) {
            return CompilationMapper.toCompilationResponse(compilation, Map.of(), Map.of());
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());

        Map<String, Long> viewsMap;
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(10),
                    LocalDateTime.now(),
                    uris,
                    true
            );
            viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка получения статистики для подборки {}", compilation.getId(), e);
            viewsMap = Map.of();
        }

        Map<Long, Long> confirmedMap = eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        eventId -> requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                ));

        return CompilationMapper.toCompilationResponse(compilation, viewsMap, confirmedMap);
    }
}