package ru.practicum.main.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    // ===================== АДМИНСКИЙ ЗАПРОС =====================
    @Query(value = "SELECT * FROM events e " +
            "WHERE (CAST(:users AS varchar) IS NULL OR e.initiator_id IN (:users)) " +
            "AND (CAST(:states AS varchar) IS NULL OR e.state IN (:states)) " +
            "AND (CAST(:categories AS varchar) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd) " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findEventsByAdminCriteria(
            @Param("users") List<Long> users,
            @Param("states") List<String> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // ===================== ПУБЛИЧНЫЙ ПОИСК БЕЗ ПАГИНАЦИИ =====================
    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:categories AS varchar) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS varchar) IS NULL OR e.paid = :paid) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd) " +
            "AND (CAST(:text AS varchar) IS NULL OR :text = '' OR " +
            "LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 OR " +
            "(SELECT COUNT(r.id) FROM requests r WHERE r.event_id = e.id AND" +
            " r.status = 'CONFIRMED') < e.participant_limit) " +
            "ORDER BY e.event_date",
            nativeQuery = true)
    List<Event> findAllPublishedEventsWithText(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable
    );

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:categories AS varchar) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS varchar) IS NULL OR e.paid = :paid) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 OR " +
            "(SELECT COUNT(r.id) FROM requests r WHERE r.event_id = e.id AND" +
            " r.status = 'CONFIRMED') < e.participant_limit) " +
            "ORDER BY e.event_date",
            nativeQuery = true)
    List<Event> findAllPublishedEventsWithoutText(
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable
    );

    // ===================== ПУБЛИЧНЫЙ ПОИСК С ПАГИНАЦИЕЙ =====================
    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:categories AS varchar) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS varchar) IS NULL OR e.paid = :paid) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd) " +
            "AND (CAST(:text AS varchar) IS NULL OR :text = '' OR " +
            "LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 OR " +
            "(SELECT COUNT(r.id) FROM requests r WHERE r.event_id = e.id AND" +
            " r.status = 'CONFIRMED') < e.participant_limit) " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findPublishedEventsByCriteria(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:categories AS varchar) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS varchar) IS NULL OR e.paid = :paid) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 OR " +
            "(SELECT COUNT(r.id) FROM requests r WHERE r.event_id = e.id AND" +
            " r.status = 'CONFIRMED') < e.participant_limit) " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findPublishedEventsByCriteriaWithoutText(
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.category.id = :categoryId")
    boolean existsEventsByCategoryId(@Param("categoryId") Long categoryId);
}