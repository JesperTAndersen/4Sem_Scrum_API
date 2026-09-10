package app.dao;

import java.util.List;

public interface IReadDAO<T>
{
    T get(Long id);

    List<T> getAll();
}

