package ru.practicum.main.service;

import ru.practicum.main.dto.request.NewCompilationRequest;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationResponse;

import java.util.List;

public interface CompilationService {
    CompilationResponse addCompilation(NewCompilationRequest request);

    void deleteCompilation(Long compId);

    CompilationResponse updateCompilation(Long compId, UpdateCompilationRequest request);

    List<CompilationResponse> getCompilations(Boolean pinned, Integer from, Integer size);

    CompilationResponse getCompilation(Long compId);
}