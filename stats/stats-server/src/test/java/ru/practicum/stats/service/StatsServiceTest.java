package ru.practicum.stats.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private EndpointHitDto hitDto;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        start = LocalDateTime.parse("2026-06-19 10:00:00", formatter);
        end = LocalDateTime.parse("2026-06-19 23:00:00", formatter);

        hitDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void saveHit_ShouldSaveSuccessfully() {

        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();

        when(statsRepository.save(any(EndpointHit.class))).thenReturn(hit);

        statsService.saveHit(hitDto);

        verify(statsRepository, times(1)).save(any(EndpointHit.class));
    }

    @Test
    void getStats_ShouldReturnStatsWithDuplicates() {
        List<String> uris = List.of("/test/1");
        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("test-app", "/test/1", 5L)
        );

        when(statsRepository.getStatsWithDuplicates(start, end, uris))
                .thenReturn(expectedStats);

        List<ViewStatsDto> result = statsService.getStats(start, end, uris, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getHits());
        verify(statsRepository, times(1)).getStatsWithDuplicates(start, end, uris);
        verify(statsRepository, never()).getStatsWithoutDuplicates(any(), any(), anyList());
    }

    @Test
    void getStats_ShouldReturnStatsWithoutDuplicates() {
        List<String> uris = List.of("/test/1");
        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("test-app", "/test/1", 3L)
        );

        when(statsRepository.getStatsWithoutDuplicates(start, end, uris))
                .thenReturn(expectedStats);

        List<ViewStatsDto> result = statsService.getStats(start, end, uris, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getHits());
        verify(statsRepository, times(1)).getStatsWithoutDuplicates(start, end, uris);
        verify(statsRepository, never()).getStatsWithDuplicates(any(), any(), anyList());
    }

    @Test
    void getStats_ShouldThrowExceptionWhenStartAfterEnd() {
        LocalDateTime invalidStart = end.plusDays(1);

        assertThrows(BadRequestException.class, () ->
                statsService.getStats(invalidStart, end, null, false)
        );
    }

    @Test
    void getStats_ShouldReturnEmptyListWhenNoStatsFound() {
        when(statsRepository.getStatsWithDuplicates(start, end, null))
                .thenReturn(List.of());

        List<ViewStatsDto> result = statsService.getStats(start, end, null, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}