package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.model.Request;
import ru.practicum.main.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r FROM Request r WHERE r.event.id = :eventId AND r.id IN :requestIds")
    List<Request> findAllByEventIdAndIdIn(@Param("eventId") Long eventId,
                                          @Param("requestIds") List<Long> requestIds);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long userId);

    @Query("SELECT e.id, COUNT(r) FROM Event e LEFT JOIN Request r ON r.event.id = e.id AND r.status = 'CONFIRMED' " +
            "WHERE e.id IN :eventIds GROUP BY e.id")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);
}