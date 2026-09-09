package app.config;

import app.config.hibernate.HibernateConfig;
import app.controllers.SecurityController;
import app.controllers.routes.Routes;
import jakarta.persistence.EntityManagerFactory;

public class DependencyContainer
{
    private final SecurityController securityController;

    public DependencyContainer()
    {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public DependencyContainer(EntityManagerFactory emfTest)
    {
        this.securityController = new SecurityController();
    }

    public Routes getRoutes()
    {
        return new Routes(securityController);
    }
}