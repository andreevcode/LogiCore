package ru.andreevcode.logicore.corelogistics.data;

import jakarta.validation.constraints.NotNull;

public record RequestChangeCapacityDto(@NotNull Integer amount) {
}
