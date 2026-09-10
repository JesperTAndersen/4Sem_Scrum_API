package app.dao.interfaces;

public interface IReadDAO<T>
{
    T read(T entity);
}
