package app.services.entityServices;

import java.util.Set;

public interface CrudService<T> {
    T create(T t);

    T getById(Long id);

    T update(T t);

    T delete(Long id);

    Set<T> getAll();
}
