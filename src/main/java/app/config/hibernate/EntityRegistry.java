package app.config.hibernate;

import app.entities.Competence;
import org.hibernate.cfg.Configuration;

final class EntityRegistry
{

    private EntityRegistry()
    {
    }

    static void registerEntities(Configuration configuration)
    {
        configuration.addAnnotatedClass(Competence.class);
        // TODO: Add more entities here...
    }
}