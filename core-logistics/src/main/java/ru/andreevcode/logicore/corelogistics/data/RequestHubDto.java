package ru.andreevcode.logicore.corelogistics.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RequestHubDto(@NotBlank String name, @Positive int capacity, @NotBlank String code) {
}
