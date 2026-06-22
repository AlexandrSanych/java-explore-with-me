package ru.practicum.stats.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void saveHit_ShouldReturnCreated() throws Exception {

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        doNothing().when(statsService).saveHit(any(EndpointHitDto.class));


        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void saveHit_ShouldReturnBadRequest_WhenAppIsNull() throws Exception {

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();


        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_ShouldReturnStats() throws Exception {

        LocalDateTime start = LocalDateTime.parse("2026-06-19 10:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse("2026-06-19 23:00:00", formatter);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("test-app", "/test/1", 5L)
        );

        when(statsService.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(stats);


        mockMvc.perform(get("/stats")
                        .param("start", "2026-06-19 10:00:00")
                        .param("end", "2026-06-19 23:00:00")
                        .param("uris", "/test/1")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].uri").value("/test/1"))
                .andExpect(jsonPath("$[0].hits").value(5));
    }

    @Test
    void getStats_ShouldReturnEmptyArray_WhenNoStats() throws Exception {

        when(statsService.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());


        mockMvc.perform(get("/stats")
                        .param("start", "2026-06-19 10:00:00")
                        .param("end", "2026-06-19 23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStats_ShouldReturnBadRequest_WhenStartIsMissing() throws Exception {

        mockMvc.perform(get("/stats")
                        .param("end", "2026-06-19 23:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_ShouldReturnBadRequest_WhenEndIsMissing() throws Exception {

        mockMvc.perform(get("/stats")
                        .param("start", "2026-06-19 10:00:00"))
                .andExpect(status().isBadRequest());
    }
}