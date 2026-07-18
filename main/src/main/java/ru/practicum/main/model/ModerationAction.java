package ru.practicum.main.model;

public enum ModerationAction {
    SENT_TO_REVIEW,      // Отправлено на модерацию (пользователь)
    PUBLISHED,           // Опубликовано (админ)
    REJECTED,            // Отклонено (админ)
    SENT_TO_REWORK,      // Отправлено на доработку (админ)
    RESUBMITTED,         // Повторно отправлено на модерацию (пользователь)
    CANCELED             // Отменено (пользователь)
}