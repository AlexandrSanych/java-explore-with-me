package ru.practicum.main.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.NewCategoryRequest;
import ru.practicum.main.dto.response.CategoryResponse;
import ru.practicum.main.service.CategoryService;

@Slf4j
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse addCategory(@Valid @RequestBody NewCategoryRequest request) {
        log.info("Запрос на добавление категории: {}", request);
        return categoryService.addCategory(request);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        log.info("Запрос на удаление категории с id: {}", catId);
        categoryService.deleteCategory(catId);
    }

    @PatchMapping("/{catId}")
    public CategoryResponse updateCategory(@PathVariable Long catId,
                                           @Valid @RequestBody NewCategoryRequest request) {
        log.info("Запрос на обновление категории с id: {}", catId);
        return categoryService.updateCategory(catId, request);
    }
}