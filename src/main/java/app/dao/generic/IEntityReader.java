package app.dao.generic;

public interface IEntityReader<T, I>
{
    T getByID(I id);
}
