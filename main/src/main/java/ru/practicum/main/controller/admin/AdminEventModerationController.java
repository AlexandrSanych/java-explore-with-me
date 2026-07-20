package ru.practicum.main.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.ModerationRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.ModerationLogResponse;
import ru.practicum.main.service.EventModerationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventModerationController {
    private final EventModerationService moderationService;

    @PostMapping("/{eventId}/moderate")
    public EventFullDto moderateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody ModerationRequest request) {
        log.info("Запрос на модерацию события {} с действием: {}, комментарий: {}",
                eventId, request.getAction(), request.getComment());
        return moderationService.moderateEvent(eventId, request);
    }

    @GetMapping("/{eventId}/history")
    public List<ModerationLogResponse> getModerationHistory(@PathVariable Long eventId) {
        log.info("Запрос на получение истории модерации события {}", eventId);
        return moderationService.getModerationHistory(eventId);
    }
}