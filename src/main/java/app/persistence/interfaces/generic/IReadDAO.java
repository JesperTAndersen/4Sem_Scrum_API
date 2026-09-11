package app.persistence.interfaces.generic;

import java.util.List;

public interface IReadDAO<T>
{
    T get(Long id);

    List<T> getAll();
}

