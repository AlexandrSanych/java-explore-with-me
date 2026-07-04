package ru.practicum.main.service;

import ru.practicum.main.dto.request.NewCategoryRequest;
import ru.practicum.main.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse addCategory(NewCategoryRequest request);

    void deleteCategory(Long catId);

    CategoryResponse updateCategory(Long catId, NewCategoryRequest request);

    List<CategoryResponse> getCategories(Integer from, Integer size);

    CategoryResponse getCategory(Long catId);
}