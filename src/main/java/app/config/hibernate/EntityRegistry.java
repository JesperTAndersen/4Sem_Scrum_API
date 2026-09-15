package app.config.hibernate;

import org.hibernate.cfg.Configuration;

import app.entities.Competence;
import app.entities.Project;
import app.entities.Stage;
import app.entities.Task;
import app.entities.User;

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
        configuration.addAnnotatedClass(Task.class);
        configuration.addAnnotatedClass(User.class);
        // TODO: Add more entities here...
    }
}
