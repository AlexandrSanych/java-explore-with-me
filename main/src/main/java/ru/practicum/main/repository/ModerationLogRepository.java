package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.model.ModerationLog;

import java.util.List;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {
    List<ModerationLog> findAllByEventIdOrderByCreatedOnDesc(Long eventId);
}