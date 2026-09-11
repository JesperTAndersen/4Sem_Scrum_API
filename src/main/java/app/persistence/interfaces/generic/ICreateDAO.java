package app.persistence.interfaces.generic;

public interface ICreateDAO<T>
{
    T create(T t);
}
