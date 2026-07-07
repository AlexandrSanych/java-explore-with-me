package ru.practicum.main.validator;

import ru.practicum.main.exception.BadRequestException;

public class PaginationValidator {
    public static void validate(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new BadRequestException("Параметр from должен быть >= 0");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Параметр size должен быть > 0");
        }
    }
}