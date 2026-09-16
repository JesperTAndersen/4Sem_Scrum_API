package app.shared.data;

public interface IDeleteDAO<T>
{
    boolean delete(Long id);
}
