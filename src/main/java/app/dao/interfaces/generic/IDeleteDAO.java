package app.dao.interfaces.generic;

public interface IDeleteDAO<T>
{
    boolean delete(Long id);
}
