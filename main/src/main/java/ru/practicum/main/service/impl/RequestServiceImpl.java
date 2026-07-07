package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.ForbiddenException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.RequestMapper;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.Request;
import ru.practicum.main.model.RequestStatus;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.RequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        User requester = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своем событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Запрос на участие уже существует");
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие");
        }

        Request request = Request.builder()
                .event(event)
                .requester(requester)
                .created(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Создан запрос на участие: {}", savedRequest);
        return RequestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        getUserOrThrow(userId);
        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        return RequestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUserOrThrow(userId);
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId +
                        " не найден у пользователя " + userId));

        request.setStatus(RequestStatus.CANCELED);
        Request updatedRequest = requestRepository.save(request);
        log.info("Отменен запрос на участие: {}", updatedRequest);
        return RequestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }

        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return RequestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }

        List<Long> requestIds = request.getRequestIds();
        List<Request> requests = requestRepository.findAllByEventIdAndIdIn(eventId, requestIds);

        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        for (Request req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if ("CONFIRMED".equals(request.getStatus())) {
            if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит запросов на участие");
            }

            List<Request> confirmedRequestsList = new ArrayList<>();
            List<Request> rejectedRequestsList = new ArrayList<>();

            for (Request req : requests) {
                if (event.getParticipantLimit() == 0 || confirmedRequests < event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequestsList.add(req);
                    confirmedRequests++;
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedRequestsList.add(req);
                }
            }

            if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
                List<Request> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId,
                                RequestStatus.PENDING)
                        .stream()
                        .filter(r -> !requestIds.contains(r.getId()))
                        .toList();

                for (Request req : pendingRequests) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedRequestsList.add(req);
                }

                if (!pendingRequests.isEmpty()) {
                    requestRepository.saveAll(pendingRequests);
                }
            }

            requestRepository.saveAll(requests);

            result.setConfirmedRequests(RequestMapper.toParticipationRequestDtoList(confirmedRequestsList));
            result.setRejectedRequests(RequestMapper.toParticipationRequestDtoList(rejectedRequestsList));

        } else if ("REJECTED".equals(request.getStatus())) {
            for (Request req : requests) {
                req.setStatus(RequestStatus.REJECTED);
            }
            requestRepository.saveAll(requests);
            result.setRejectedRequests(RequestMapper.toParticipationRequestDtoList(requests));

        } else {
            throw new BadRequestException("Некорректный статус: " + request.getStatus());
        }

        log.info("Изменен статус заявок для события {}: {}", eventId, request.getStatus());
        return result;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }
}