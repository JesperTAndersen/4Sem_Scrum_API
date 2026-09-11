package app.utils;

import jakarta.persistence.EntityNotFoundException;

public class DBValidator
{

    private DBValidator(){}

    public static <T> T validateExists(T entity, Object id, Class<T> entityClass)
    {
        if (entity == null)
        {
            String className = entityClass.getSimpleName();
            throw new EntityNotFoundException(className + " with ID " + id + " was not found.");
        }
        return entity;
    }
}
