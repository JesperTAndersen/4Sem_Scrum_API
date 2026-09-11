package app.config.hibernate;

import app.entities.*;
import org.hibernate.cfg.Configuration;

final class EntityRegistry
{

    private EntityRegistry()
    {
    }

    static void registerEntities(Configuration configuration)
    {
        configuration.addAnnotatedClass(Competence.class);
        configuration.addAnnotatedClass(Project.class);
        configuration.addAnnotatedClass(Stage.class);
        configuration.addAnnotatedClass(User.class);
        // TODO: Add more entities here...
    }
}
