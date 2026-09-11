package app.dao.interfaces.generic;

public interface ICrudDAO<T> extends ICreateDAO<T>, IReadDAO<T>, IUpdateDAO<T>, IDeleteDAO<T>
{
}