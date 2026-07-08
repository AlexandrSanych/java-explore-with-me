package ru.practicum.main.controller.pub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.response.CategoryResponse;
import ru.practicum.main.service.CategoryService;
import ru.practicum.main.validator.PaginationValidator;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> getCategories(@RequestParam(defaultValue = "0") Integer from,
                                                @RequestParam(defaultValue = "10") Integer size) {
        PaginationValidator.validate(from, size);
        log.info("Запрос на получение категорий с параметрами: from={}, size={}", from, size);
        return categoryService.getCategories(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryResponse getCategory(@PathVariable Long catId) {
        log.info("Запрос на получение категории с id: {}", catId);
        return categoryService.getCategory(catId);
    }
}