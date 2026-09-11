package app.persistence.interfaces.generic;

public interface IUpdateDAO<T>
{
    T update(T t);
}
