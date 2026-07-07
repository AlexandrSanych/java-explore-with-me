package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.main.dto.request.NewCompilationRequest;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Event createEvent(Long id, String title) {
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        User user = User.builder()
                .id(1L)
                .name("Test User")
                .build();

        return Event.builder()
                .id(id)
                .title(title)
                .annotation("Test annotation")
                .category(category)
                .initiator(user)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();
    }

    @Test
    void addCompilation_shouldCreateAndReturnCompilation() {
        NewCompilationRequest request = NewCompilationRequest.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(1L, 2L))
                .build();

        Event event1 = createEvent(1L, "Event 1");
        Event event2 = createEvent(2L, "Event 2");
        List<Event> events = List.of(event1, event2);

        Compilation compilation = Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(events)
                .build();

        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(false);
        when(eventRepository.findAllById(List.of(1L, 2L))).thenReturn(events);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), anyBoolean()))
                .thenReturn(List.of());

        CompilationResponse response = compilationService.addCompilation(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Compilation");
        assertThat(response.getPinned()).isTrue();

        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }

    @Test
    void addCompilation_shouldThrowConflictException_whenTitleExists() {
        NewCompilationRequest request = NewCompilationRequest.builder()
                .title("Existing Compilation")
                .build();

        when(compilationRepository.existsByTitle("Existing Compilation")).thenReturn(true);

        assertThatThrownBy(() -> compilationService.addCompilation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Подборка с таким названием уже существует");

        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void addCompilation_shouldCreateCompilationWithoutEvents_whenEventsIsEmpty() {
        NewCompilationRequest request = NewCompilationRequest.builder()
                .title("Compilation Without Events")
                .pinned(false)
                .events(List.of())
                .build();

        Compilation compilation = Compilation.builder()
                .id(1L)
                .title("Compilation Without Events")
                .pinned(false)
                .events(List.of())
                .build();

        when(compilationRepository.existsByTitle("Compilation Without Events")).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);

        CompilationResponse response = compilationService.addCompilation(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Compilation Without Events");
        assertThat(response.getPinned()).isFalse();
    }

    @Test
    void getCompilation_shouldReturnCompilation_whenExists() {
        Long compilationId = 1L;
        Compilation compilation = Compilation.builder()
                .id(compilationId)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(compilation));

        CompilationResponse response = compilationService.getCompilation(compilationId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(compilationId);
        assertThat(response.getTitle()).isEqualTo("Test Compilation");
        assertThat(response.getPinned()).isTrue();
    }

    @Test
    void getCompilation_shouldThrowNotFoundException_whenNotExists() {
        Long compilationId = 999L;
        when(compilationRepository.findById(compilationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getCompilation(compilationId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка с id " + compilationId + " не найдена");
    }

    @Test
    void getCompilations_shouldReturnAllCompilations_whenPinnedIsNull() {
        Compilation compilation1 = Compilation.builder()
                .id(1L)
                .title("Compilation 1")
                .pinned(true)
                .events(List.of())
                .build();

        Compilation compilation2 = Compilation.builder()
                .id(2L)
                .title("Compilation 2")
                .pinned(false)
                .events(List.of())
                .build();

        Page<Compilation> page = new PageImpl<>(List.of(compilation1, compilation2));

        when(compilationRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<CompilationResponse> result = compilationService.getCompilations(null, 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Compilation 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Compilation 2");
    }

    @Test
    void getCompilations_shouldReturnPinnedCompilations_whenPinnedIsTrue() {
        Compilation compilation = Compilation.builder()
                .id(1L)
                .title("Pinned Compilation")
                .pinned(true)
                .events(List.of())
                .build();
        when(compilationRepository.findAllByPinned(eq(true), any(Pageable.class)))
                .thenReturn(List.of(compilation));

        List<CompilationResponse> result = compilationService.getCompilations(true, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPinned()).isTrue();
    }

    @Test
    void getCompilations_shouldReturnNonPinnedCompilations_whenPinnedIsFalse() {
        Compilation compilation = Compilation.builder()
                .id(1L)
                .title("Non Pinned Compilation")
                .pinned(false)
                .events(List.of())
                .build();
        when(compilationRepository.findAllByPinned(eq(false), any(Pageable.class)))
                .thenReturn(List.of(compilation));

        List<CompilationResponse> result = compilationService.getCompilations(false, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPinned()).isFalse();
    }

    @Test
    void deleteCompilation_shouldDeleteCompilation_whenExists() {
        Long compilationId = 1L;
        Compilation compilation = Compilation.builder()
                .id(compilationId)
                .title("Test Compilation")
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(compilationId);

        verify(compilationRepository, times(1)).delete(compilation);
    }

    @Test
    void deleteCompilation_shouldThrowNotFoundException_whenNotExists() {
        Long compilationId = 999L;
        when(compilationRepository.findById(compilationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.deleteCompilation(compilationId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка с id " + compilationId + " не найдена");
    }

    @Test
    void updateCompilation_shouldUpdateTitle_whenTitleIsProvided() {
        Long compilationId = 1L;
        UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .build();

        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .title("Old Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(existingCompilation);

        CompilationResponse response = compilationService.updateCompilation(compilationId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Compilation");
    }

    @Test
    void updateCompilation_shouldThrowConflictException_whenTitleAlreadyExists() {
        Long compilationId = 1L;
        UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                .title("Existing Title")
                .build();

        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .title("Old Title")
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.existsByTitle("Existing Title")).thenReturn(true);

        assertThatThrownBy(() -> compilationService.updateCompilation(compilationId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Подборка с таким названием уже существует");
    }

    @Test
    void updateCompilation_shouldUpdatePinned_whenPinnedIsProvided() {
        Long compilationId = 1L;
        UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                .pinned(false)
                .build();

        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(existingCompilation);

        CompilationResponse response = compilationService.updateCompilation(compilationId, request);

        assertThat(response).isNotNull();
        assertThat(response.getPinned()).isFalse();
    }

    @Test
    void updateCompilation_shouldUpdateEvents_whenEventsIsProvided() {
        Long compilationId = 1L;
        UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                .events(List.of(5L, 6L))
                .build();

        Event event5 = createEvent(5L, "Event 5");
        Event event6 = createEvent(6L, "Event 6");
        List<Event> newEvents = List.of(event5, event6);

        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(eventRepository.findAllById(List.of(5L, 6L))).thenReturn(newEvents);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(existingCompilation);

        CompilationResponse response = compilationService.updateCompilation(compilationId, request);

        assertThat(response).isNotNull();
        verify(eventRepository, times(1)).findAllById(List.of(5L, 6L));
    }

    @Test
    void addCompilation_shouldThrowNotFoundException_whenSomeEventsNotFound() {
        NewCompilationRequest request = NewCompilationRequest.builder()
                .title("Test Compilation")
                .events(List.of(1L, 999L))
                .build();

        Event event1 = createEvent(1L, "Event 1");
        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(false);
        when(eventRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(event1));

        assertThatThrownBy(() -> compilationService.addCompilation(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("События с id [999] не найдены");

        verify(compilationRepository, never()).save(any(Compilation.class));
    }
}