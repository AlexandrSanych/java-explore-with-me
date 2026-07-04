package ru.practicum.main.mapper;

import ru.practicum.main.dto.request.NewCategoryRequest;
import ru.practicum.main.dto.response.CategoryResponse;
import ru.practicum.main.model.Category;

public class CategoryMapper {
    public static Category toCategory(NewCategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .build();
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}