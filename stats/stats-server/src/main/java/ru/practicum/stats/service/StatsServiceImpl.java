package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();

        statsRepository.save(hit);
        log.info("Сохранен запрос к эндпоинту: {}", hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        // 1. Проверка, что дата начала не позже даты окончания
        if (start.isAfter(end)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        // 2. Проверка на пустой список uris - возвращаем пустой результат
        if (uris != null && uris.isEmpty()) {
            log.info("Передан пустой список uris, возвращаем пустую статистику");
            return Collections.emptyList();
        }

        // 3. Получение статистики в зависимости от параметра unique
        List<ViewStatsDto> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = statsRepository.getStatsWithoutDuplicates(start, end, uris);
        } else {
            stats = statsRepository.getStatsWithDuplicates(start, end, uris);
        }

        log.info("Получена статистика. Количество записей: {}", stats.size());
        return stats;
    }
}