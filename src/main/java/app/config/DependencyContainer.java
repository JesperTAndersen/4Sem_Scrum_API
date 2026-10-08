package app.config;

import app.capacity.data.CompanyCapacityDAO;
import app.capacity.data.ICompanyCapacityDAO;
import app.capacity.domain.CompanyCapacityService;
import app.capacity.domain.ICompanyCapacityService;
import app.capacity.presentation.CompanyCapacityController;
import app.capacity.presentation.ICompanyCapacityController;
import app.competence.data.CompetenceDAO;
import app.competence.data.ICompetenceDAO;
import app.competence.domain.CompetenceService;
import app.competence.domain.ICompetenceService;
import app.competence.presentation.CompetenceController;
import app.competence.presentation.ICompetenceController;
import app.config.hibernate.HibernateConfig;
import app.employee.data.EmployeeDAO;
import app.employee.data.IEmployeeDAO;
import app.employee.domain.EmployeeService;
import app.employee.domain.IEmployeeService;
import app.employee.presentation.EmployeeController;
import app.employee.presentation.IEmployeeController;
import app.presentation.health.HealthCheckController;
import app.presentation.health.IHealthCheckController;
import app.project.data.IProjectDAO;
import app.project.data.ProjectDAO;
import app.project.domain.IProjectService;
import app.project.domain.IScheduleService;
import app.project.domain.ProjectService;
import app.project.domain.ScheduleService;
import app.project.presentation.ProjectController;
import app.security.domain.ISecurityService;
import app.security.domain.SecurityService;
import app.security.presentation.ISecurityController;
import app.security.presentation.SecurityController;
import app.shared.presentation.ICrudController;
import app.stage.data.IStageDAO;
import app.stage.data.StageDAO;
import app.stage.domain.IStageService;
import app.stage.domain.StageService;
import app.stage.presentation.StageController;
import app.task.data.TaskDAO;
import app.task.domain.ITaskService;
import app.task.domain.TaskService;
import app.task.presentation.TaskController;
import app.user.data.IUserDAO;
import app.user.data.UserDAO;
import app.user.domain.IUserService;
import app.user.domain.UserService;
import app.user.presentation.IUserController;
import app.user.presentation.UserController;
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
    private final TaskDAO taskDAO;
    private final IProjectDAO projectDAO;
    private final IEmployeeDAO employeeDAO;
    private final ICompanyCapacityDAO companyCapacityDAO;

    @Getter
    private final ICompanyCapacityService companyCapacityService;
    @Getter
    private final ICompanyCapacityController companyCapacityController;

    @Getter
    private final IUserService userService;
    @Getter
    private final ICompetenceService competenceService;
    @Getter
    private final IHealthCheckController healthCheckController;
    @Getter
    private final IUserController userController;
    @Getter
    private final ICompetenceController competenceController;
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
    private final TaskController taskController;
    @Getter
    private final IScheduleService scheduleService;
    @Getter
    private final IEmployeeService employeeService;
    @Getter
    private final IEmployeeController employeeController;

    public DependencyContainer(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.healthCheckController = new HealthCheckController(entityManagerFactory);

        this.companyCapacityDAO = new CompanyCapacityDAO(entityManagerFactory);
        this.companyCapacityDAO.initialize();
        this.companyCapacityService = new CompanyCapacityService(companyCapacityDAO);
        this.companyCapacityController = new CompanyCapacityController(companyCapacityService);

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
        this.scheduleService = new ScheduleService();
        this.projectService = new ProjectService(projectDAO, userDAO, scheduleService);
        this.projectController = new ProjectController(projectService);
        this.securityService = new SecurityService(userDAO);
        this.securityController = new SecurityController(securityService);
        this.taskDAO = new TaskDAO(entityManagerFactory);
        this.taskService = new TaskService(taskDAO, stageDAO, competenceDAO);
        this.taskController = new TaskController(taskService);

        this.employeeDAO = new EmployeeDAO(entityManagerFactory);
        this.employeeService = new EmployeeService(employeeDAO, companyCapacityDAO, competenceDAO);
        this.employeeController = new EmployeeController(employeeService);
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