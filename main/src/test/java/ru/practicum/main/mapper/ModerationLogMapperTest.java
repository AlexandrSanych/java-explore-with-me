package ru.practicum.main.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.main.dto.response.ModerationLogResponse;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.ModerationAction;
import ru.practicum.main.model.ModerationLog;
import ru.practicum.main.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationLogMapperTest {

    @Test
    void toModerationLogResponse_shouldMapCorrectly() {
        User moderator = User.builder()
                .id(1L)
                .name("Admin User")
                .build();

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .build();

        ModerationLog log = ModerationLog.builder()
                .id(1L)
                .event(event)
                .moderator(moderator)
                .action(ModerationAction.PUBLISHED)
                .comment("Published successfully")
                .createdOn(LocalDateTime.now())
                .build();

        ModerationLogResponse response = ModerationLogMapper.toModerationLogResponse(log);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventId()).isEqualTo(1L);
        assertThat(response.getModerator().getId()).isEqualTo(1L);
        assertThat(response.getModerator().getName()).isEqualTo("Admin User");
        assertThat(response.getAction()).isEqualTo(ModerationAction.PUBLISHED);
        assertThat(response.getComment()).isEqualTo("Published successfully");
    }

    @Test
    void toModerationLogResponseList_shouldMapCorrectly() {
        User moderator = User.builder()
                .id(1L)
                .name("Admin User")
                .build();

        Event event = Event.builder()
                .id(1L)
                .build();

        ModerationLog log1 = ModerationLog.builder()
                .id(1L)
                .event(event)
                .moderator(moderator)
                .action(ModerationAction.SENT_TO_REWORK)
                .comment("Please fix")
                .createdOn(LocalDateTime.now())
                .build();

        ModerationLog log2 = ModerationLog.builder()
                .id(2L)
                .event(event)
                .moderator(moderator)
                .action(ModerationAction.RESUBMITTED)
                .comment("Resubmitted after rework")
                .createdOn(LocalDateTime.now().plusHours(1))
                .build();

        List<ModerationLogResponse> responses = ModerationLogMapper.toModerationLogResponseList(List.of(log1, log2));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAction()).isEqualTo(ModerationAction.SENT_TO_REWORK);
        assertThat(responses.get(1).getAction()).isEqualTo(ModerationAction.RESUBMITTED);
    }

    @Test
    void toModerationLogResponse_shouldReturnNull_whenLogIsNull() {
        ModerationLogResponse response = ModerationLogMapper.toModerationLogResponse(null);
        assertThat(response).isNull();
    }

    @Test
    void toModerationLogResponseList_shouldReturnEmptyList_whenLogsIsNull() {
        List<ModerationLogResponse> responses = ModerationLogMapper.toModerationLogResponseList(null);
        assertThat(responses).isEmpty();
    }

    @Test
    void toModerationLogResponseList_shouldReturnEmptyList_whenLogsIsEmpty() {
        List<ModerationLogResponse> responses = ModerationLogMapper.toModerationLogResponseList(List.of());
        assertThat(responses).isEmpty();
    }
}