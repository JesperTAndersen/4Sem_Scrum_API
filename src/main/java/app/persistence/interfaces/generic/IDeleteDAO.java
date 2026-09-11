package app.persistence.interfaces.generic;

public interface IDeleteDAO<T>
{
    boolean delete(Long id);
}
