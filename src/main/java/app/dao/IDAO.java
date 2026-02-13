package app.daos;

import java.util.Set;

public interface IDAO <T> {
    T create(T t);

    T getById(int id);

    T update(T t);

    T delete(int id);

    Set<T> getAll();
}