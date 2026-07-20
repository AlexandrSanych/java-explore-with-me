package ru.practicum.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRequest {
    @NotBlank(message = "Действие обязательно")
    private String action;

    @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
    private String comment;
}