package app.dao.generic;

import app.entities.IEntity;

public interface
IEntityDAO<T extends IEntity, I> extends IEntityReader<T, I>
{
    T create(T t);

    T update(T t);

    boolean delete(I id);
}
