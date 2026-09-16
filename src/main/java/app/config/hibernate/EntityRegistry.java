package app.config.hibernate;

import org.hibernate.cfg.Configuration;

import app.competence.domain.Competence;
import app.project.domain.Project;
import app.stage.domain.Stage;
import app.task.domain.Task;
import app.user.domain.User;

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
