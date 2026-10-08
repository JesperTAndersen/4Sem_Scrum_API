package app.presentation;

import app.capacity.presentation.CompanyCapacityRoutes;
import app.competence.presentation.CompetenceRoutes;
import app.employee.presentation.EmployeeRoutes;
import app.presentation.health.HealthCheckRoute;
import app.project.presentation.ProjectRoutes;
import app.security.domain.Role;
import app.security.presentation.SecurityRoutes;
import app.stage.presentation.StageRoutes;
import app.task.presentation.TaskRoutes;
import app.user.presentation.UserRoutes;
import io.javalin.apibuilder.EndpointGroup;
import lombok.Getter;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes
{
    @Getter
    private static final String API_VERSION = "api/v1";
    private final HealthCheckRoute healthCheckRoute;
    private final UserRoutes userRoutes;
    private final StageRoutes stageRoutes;
    private final TaskRoutes taskRoutes;
    private final CompetenceRoutes competenceRoutes;
    private final ProjectRoutes projectRoutes;
    private final SecurityRoutes securityRoutes;
    private final EmployeeRoutes employeeRoutes;
    private final CompanyCapacityRoutes companyCapacityRoutes;

    public Routes(HealthCheckRoute healthCheckRoute, UserRoutes userRoutes, StageRoutes stageRoutes,
                  TaskRoutes taskRoutes, CompetenceRoutes competenceRoutes, ProjectRoutes projectRoutes,
                  SecurityRoutes securityRoutes, EmployeeRoutes employeeRoutes,
                  CompanyCapacityRoutes companyCapacityRoutes)
    {
        this.healthCheckRoute = healthCheckRoute;
        this.userRoutes = userRoutes;
        this.stageRoutes = stageRoutes;
        this.taskRoutes = taskRoutes;
        this.competenceRoutes = competenceRoutes;
        this.projectRoutes = projectRoutes;
        this.securityRoutes = securityRoutes;
        this.employeeRoutes = employeeRoutes;
        this.companyCapacityRoutes = companyCapacityRoutes;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            get("/", ctx -> ctx.status(200).json(Map.of("message", "Welcome to the Scrum API!")), Role.ANYONE);

            path(API_VERSION, () ->
            {
                healthCheckRoute.getRoutes().addEndpoints();
                userRoutes.getRoutes().addEndpoints();
                stageRoutes.getRoutes().addEndpoints();
                taskRoutes.getRoutes().addEndpoints();
                competenceRoutes.getRoutes().addEndpoints();
                projectRoutes.getRoutes().addEndpoints();
                securityRoutes.getRoutes().addEndpoints();
                employeeRoutes.getRoutes().addEndpoints();
                companyCapacityRoutes.getRoutes().addEndpoints();
            });
        };
    }
}