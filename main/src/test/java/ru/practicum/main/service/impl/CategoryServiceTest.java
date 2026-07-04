package ru.practicum.main.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.request.NewCategoryRequest;
import ru.practicum.main.dto.response.CategoryResponse;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void addCategory_shouldCreateAndReturnCategory() {
        NewCategoryRequest request = NewCategoryRequest.builder()
                .name("Test Category")
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        when(categoryRepository.existsByName("Test Category")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.addCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Category");
    }

    @Test
    void getCategory_shouldThrowNotFoundException_whenNotExists() {
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(categoryId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteCategory_shouldDeleteCategory_whenNoEvents() {
        Long categoryId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.existsEventsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void deleteCategory_shouldThrowConflictException_whenHasEvents() {
        Long categoryId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.existsEventsByCategoryId(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(ConflictException.class);
    }
}