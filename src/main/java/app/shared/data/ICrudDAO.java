package app.shared.data;

public interface ICrudDAO<T> extends ICreateDAO<T>, IReadDAO<T>, IUpdateDAO<T>, IDeleteDAO<T>
{
}