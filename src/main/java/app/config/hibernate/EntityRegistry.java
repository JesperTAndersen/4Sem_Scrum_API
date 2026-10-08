package app.config.hibernate;

import app.capacity.domain.CompanyCapacity;
import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.project.domain.Project;
import app.stage.domain.Stage;
import app.task.domain.Task;
import app.user.domain.User;
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
        configuration.addAnnotatedClass(Task.class);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Employee.class);
        configuration.addAnnotatedClass(CompanyCapacity.class);
    }
}