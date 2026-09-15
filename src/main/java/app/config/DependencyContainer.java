package app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.config.hibernate.HibernateConfig;
import app.controllers.implementations.CompetenceController;
import app.controllers.implementations.HealthCheckController;
import app.controllers.implementations.ProjectController;
import app.controllers.implementations.SecurityController;
import app.controllers.implementations.StageController;
import app.controllers.implementations.TaskController;
import app.controllers.implementations.UserController;
import app.controllers.interfaces.IHealthCheckController;
import app.controllers.interfaces.ISecurityController;
import app.controllers.interfaces.IUserController;
import app.controllers.interfaces.generic.ICrudController;
import app.persistence.implementations.CompetenceDAO;
import app.persistence.implementations.ProjectDAO;
import app.persistence.implementations.StageDAO;
import app.persistence.implementations.TaskDAO;
import app.persistence.implementations.UserDAO;
import app.persistence.interfaces.specific.ICompetenceDAO;
import app.persistence.interfaces.specific.IProjectDAO;
import app.persistence.interfaces.specific.IStageDAO;
import app.persistence.interfaces.specific.IUserDAO;
import app.services.implementations.CompetenceService;
import app.services.implementations.ProjectService;
import app.services.implementations.SecurityService;
import app.services.implementations.StageService;
import app.services.implementations.TaskService;
import app.services.implementations.UserService;
import app.services.interfaces.ICompetenceService;
import app.services.interfaces.IProjectService;
import app.services.interfaces.ISecurityService;
import app.services.interfaces.IStageService;
import app.services.interfaces.ITaskService;
import app.services.interfaces.IUserService;
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
        this.projectService = new ProjectService(projectDAO);
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
