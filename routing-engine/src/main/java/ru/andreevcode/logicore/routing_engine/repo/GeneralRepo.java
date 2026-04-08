package ru.andreevcode.logicore.routing_engine.repo;

import java.util.List;
import java.util.Optional;

public interface GeneralRepo<T, R> {
    List<T> findAll();

    Optional<T> findById(R id);

    int insert(T t);
}
