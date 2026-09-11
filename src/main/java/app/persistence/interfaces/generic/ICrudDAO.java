package app.persistence.interfaces.generic;

public interface ICrudDAO<T> extends ICreateDAO<T>, IReadDAO<T>, IUpdateDAO<T>, IDeleteDAO<T>
{
}