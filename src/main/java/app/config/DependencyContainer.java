package app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.config.hibernate.HibernateConfig;
import app.competence.presentation.CompetenceController;
import app.presentation.health.HealthCheckController;
import app.project.presentation.ProjectController;
import app.security.presentation.SecurityController;
import app.stage.presentation.StageController;
import app.task.presentation.TaskController;
import app.user.presentation.UserController;
import app.presentation.health.IHealthCheckController;
import app.security.presentation.ISecurityController;
import app.user.presentation.IUserController;
import app.shared.presentation.ICrudController;
import app.competence.data.CompetenceDAO;
import app.project.data.ProjectDAO;
import app.stage.data.StageDAO;
import app.task.data.TaskDAO;
import app.user.data.UserDAO;
import app.competence.data.ICompetenceDAO;
import app.project.data.IProjectDAO;
import app.stage.data.IStageDAO;
import app.user.data.IUserDAO;
import app.competence.domain.CompetenceService;
import app.project.domain.ProjectService;
import app.security.domain.SecurityService;
import app.stage.domain.StageService;
import app.task.domain.TaskService;
import app.user.domain.UserService;
import app.competence.domain.ICompetenceService;
import app.project.domain.IProjectService;
import app.security.domain.ISecurityService;
import app.stage.domain.IStageService;
import app.task.domain.ITaskService;
import app.user.domain.IUserService;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;

public final class DependencyContainer
{
    private static DependencyContainer instance;
    private final EntityManagerFactory entityManagerFactory;
    @Getter
    private final ObjectMapper objectMapper;
    private final IUserDAO userDAO;
    private final ICompetenceDAO competenceDAO;
    private final IStageDAO stageDAO;
    private final TaskDAO taskDAO;
    private final IProjectDAO projectDAO;

    @Getter
    private final IUserService userService;
    @Getter
    private final ICompetenceService competenceService;
    @Getter
    private final IHealthCheckController healthCheckController;
    @Getter
    private final IUserController userController;
    @Getter
    private final ICrudController competenceController;
    @Getter
    private final IStageService stageService;
    @Getter
    private final ICrudController stageController;
    @Getter
    private final IProjectService projectService;
    @Getter
    private final ICrudController projectController;
    @Getter
    private final ISecurityController securityController;
    @Getter
    private final ISecurityService securityService;
    @Getter
    private final ITaskService taskService;
    @Getter
    private final ICrudController taskController;

    public DependencyContainer(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.healthCheckController = new HealthCheckController(entityManagerFactory);

        this.userDAO = new UserDAO(entityManagerFactory);
        this.userService = new UserService(userDAO);
        this.userController = new UserController(userService);

        this.competenceDAO = new CompetenceDAO(entityManagerFactory);
        this.competenceService = new CompetenceService(competenceDAO);
        this.competenceController = new CompetenceController(competenceService);

        this.stageDAO = new StageDAO(entityManagerFactory);
        this.projectDAO = new ProjectDAO(entityManagerFactory);
        this.stageService = new StageService(stageDAO, projectDAO);
        this.stageController = new StageController(stageService);
        this.projectService = new ProjectService(projectDAO, userDAO);
        this.projectController = new ProjectController(projectService);
        this.securityService = new SecurityService(userDAO);
        this.securityController = new SecurityController(securityService);
        this.taskDAO = new TaskDAO(entityManagerFactory);
        this.taskService = new TaskService(taskDAO, stageDAO);
        this.taskController = new TaskController(taskService);
    }

    public static DependencyContainer getInstance()
    {
        if (instance == null)
        {
            instance = new DependencyContainer(HibernateConfig.getEntityManagerFactory());
        }
        return instance;
    }

    public static DependencyContainer getTestInstance(EntityManagerFactory entityManagerFactory)
    {
        instance = new DependencyContainer(entityManagerFactory);
        return instance;
    }
}
