package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.NewCategoryRequest;
import ru.practicum.main.dto.response.CategoryResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CategoryMapper;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.CategoryService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryResponse addCategory(NewCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Категория с таким именем уже существует");
        }

        Category category = CategoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);
        log.info("Добавлена новая категория: {}", savedCategory);
        return CategoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryOrThrow(catId);

        if (eventRepository.existsEventsByCategoryId(catId)) {
            throw new ConflictException("Нельзя удалить категорию, с которой связаны события");
        }

        categoryRepository.delete(category);
        log.info("Удалена категория с id: {}", catId);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long catId, NewCategoryRequest request) {
        Category category = getCategoryOrThrow(catId);

        if (categoryRepository.existsByName(request.getName())
                && !category.getName().equals(request.getName())) {
            throw new ConflictException("Категория с таким именем уже существует");
        }

        category.setName(request.getName());
        Category updatedCategory = categoryRepository.save(category);
        log.info("Обновлена категория: {}", updatedCategory);
        return CategoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "id"));

        return categoryRepository.findAll(pageable)
                .stream()
                .map(CategoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long catId) {
        Category category = getCategoryOrThrow(catId);
        return CategoryMapper.toCategoryResponse(category);
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
    }
}