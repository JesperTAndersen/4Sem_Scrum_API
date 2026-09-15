package app.config;

import app.config.hibernate.HibernateConfig;
import app.controllers.implementations.*;
import app.controllers.interfaces.IHealthCheckController;
import app.controllers.interfaces.IUserController;
import app.controllers.interfaces.generic.ICrudController;
import app.persistence.implementations.CompetenceDAO;
import app.persistence.implementations.ProjectDAO;
import app.persistence.implementations.StageDAO;
import app.persistence.implementations.UserDAO;
import app.persistence.interfaces.specific.ICompetenceDAO;
import app.persistence.interfaces.specific.IProjectDAO;
import app.persistence.interfaces.specific.IStageDAO;
import app.persistence.interfaces.specific.IUserDAO;
import app.services.implementations.CompetenceService;
import app.services.implementations.ProjectService;
import app.services.implementations.StageService;
import app.services.implementations.UserService;
import app.services.interfaces.ICompetenceService;
import app.services.interfaces.IProjectService;
import app.services.interfaces.IStageService;
import app.services.interfaces.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
