package app.dao.interfaces;

public interface ICreateDAO<T>
{
    T create(T entity);
}
