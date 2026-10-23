package app.persistence.testdoubles;

import java.lang.reflect.Field;

/**
 * Assigns the database-generated ID that entities without an ID setter would get from a real DAO.
 */
public final class EntityIds
{
    private EntityIds()
    {
    }

    public static <T> T assign(T entity, Long id)
    {
        try
        {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Could not assign test ID to " + entity.getClass().getSimpleName(), e);
        }
    }
}
