package ru.practicum.main.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.main.service.EventStatsService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatsServiceImpl implements EventStatsService {

    private final StatsClient statsClient;
    private final HttpServletRequest httpServletRequest;

    private static final LocalDateTime STATS_START = LocalDateTime.of(2020, 1, 1,
            0, 0, 0);

    @Override
    public Map<String, Long> getEventViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        try {
            LocalDateTime start = STATS_START;
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

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

    @Override
    public Long getViewsForEvent(Long eventId) {
        Map<String, Long> viewsMap = getEventViewsMap(List.of(eventId));
        return viewsMap.getOrDefault("/events/" + eventId, 0L);
    }

    @Override
    public void saveHit(String uri) {
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

        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage(), e);
        }
    }
}