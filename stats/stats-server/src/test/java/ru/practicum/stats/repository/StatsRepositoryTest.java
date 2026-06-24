package ru.practicum.stats.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StatsRepositoryTest {

    @Autowired
    private StatsRepository statsRepository;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        start = LocalDateTime.parse("2026-06-19 10:00:00", formatter);
        end = LocalDateTime.parse("2026-06-19 23:00:00", formatter);

        EndpointHit hit1 = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.parse("2026-06-19 12:00:00", formatter))
                .build();

        EndpointHit hit2 = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.2")
                .timestamp(LocalDateTime.parse("2026-06-19 13:00:00", formatter))
                .build();

        EndpointHit hit3 = EndpointHit.builder()
                .app("test-app")
                .uri("/test/2")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.parse("2026-06-19 14:00:00", formatter))
                .build();

        statsRepository.save(hit1);
        statsRepository.save(hit2);
        statsRepository.save(hit3);
    }

    @Test
    void getStatsWithDuplicates_ShouldReturnStatsWithAllHits() {

        List<ViewStatsDto> result = statsRepository.getStatsWithDuplicates(
                start, end, List.of("/test/1")
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getHits());
        assertEquals("/test/1", result.get(0).getUri());
    }

    @Test
    void getStatsWithoutDuplicates_ShouldReturnStatsWithUniqueHits() {

        List<ViewStatsDto> result = statsRepository.getStatsWithoutDuplicates(
                start, end, List.of("/test/1")
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getHits());
        assertEquals("/test/1", result.get(0).getUri());
    }

    @Test
    void getStatsWithDuplicates_ShouldReturnAllStats_WhenUrisIsNull() {

        List<ViewStatsDto> result = statsRepository.getStatsWithDuplicates(start, end, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getStatsWithoutDuplicates_ShouldReturnAllStats_WhenUrisIsNull() {

        List<ViewStatsDto> result = statsRepository.getStatsWithoutDuplicates(start, end, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getStatsWithDuplicates_ShouldReturnEmpty_WhenNoMatches() {

        List<ViewStatsDto> result = statsRepository.getStatsWithDuplicates(
                start, end, List.of("/nonexistent")
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}