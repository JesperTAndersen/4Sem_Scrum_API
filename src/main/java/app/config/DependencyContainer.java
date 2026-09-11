package app.config;

import app.config.hibernate.HibernateConfig;
import app.controllers.CompetenceController;
import app.controllers.SecurityController;
import app.controllers.routes.Routes;
import app.dao.implementations.CompetenceDAO;
import app.services.implementations.CompetenceService;
import jakarta.persistence.EntityManagerFactory;

public class DependencyContainer
{
    private final SecurityController securityController;
    private final CompetenceController competenceController;

    public DependencyContainer()
    {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public DependencyContainer(EntityManagerFactory emfTest)
    {
        CompetenceDAO competenceDAO = new CompetenceDAO(emfTest);
        CompetenceService competenceService = new CompetenceService(competenceDAO);

        this.competenceController = new CompetenceController(competenceService);
        this.securityController = new SecurityController();
    }

    public Routes getRoutes()
    {
        return new Routes(securityController, competenceController);
    }
}
