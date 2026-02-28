package app.dao;

import java.util.Set;

public interface IDAO <T> {
    T create(T t);

    T getById(Long id);

    T update(T t);

    T delete(Long id);

    Set<T> getAll();
}