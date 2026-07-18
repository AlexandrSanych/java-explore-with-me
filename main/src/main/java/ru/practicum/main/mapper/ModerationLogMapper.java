package ru.practicum.main.mapper;

import ru.practicum.main.dto.response.ModerationLogResponse;
import ru.practicum.main.model.ModerationLog;

import java.util.List;
import java.util.stream.Collectors;

public class ModerationLogMapper {

    public static ModerationLogResponse toModerationLogResponse(ModerationLog log) {
        if (log == null) {
            return null;
        }
        return ModerationLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEvent().getId())
                .moderator(UserMapper.toUserShortDto(log.getModerator()))
                .action(log.getAction())
                .comment(log.getComment())
                .createdOn(log.getCreatedOn())
                .build();
    }

    public static List<ModerationLogResponse> toModerationLogResponseList(List<ModerationLog> logs) {
        if (logs == null) {
            return List.of();
        }
        return logs.stream()
                .map(ModerationLogMapper::toModerationLogResponse)
                .collect(Collectors.toList());
    }
}