package ru.andreevcode.logicore.corelogistics.repo;

import java.util.List;
import java.util.Optional;

public interface GeneralRepo<T, R> {
    List<T> findAll();

    Optional<T> findById(R id);

    T insert(T t);
}
