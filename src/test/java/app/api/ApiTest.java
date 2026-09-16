package app.api;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import app.config.ApplicationConfig;
import app.config.HibernateTestConfig;
import app.persistence.testutils.TestPopulator;
import app.security.domain.Role;
import app.utils.JWTUtil;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base API test.
 */
class ApiTest implements BeforeAllCallback, AutoCloseable
{
    static String JWT_TOKEN;
    private static PostgreSQLContainer postgres;
    private static EntityManagerFactory emf;
    private static Javalin app;
    private boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context)
            throws Exception
    {
        if (started)
            return;
        started = true;

        postgres = new PostgreSQLContainer("postgres:16.2");
        postgres.withUsername("test");
        postgres.withPassword("test");
        postgres.withDatabaseName("scrum_api_test");
        postgres.start();

        emf = HibernateTestConfig.getEntityManagerFactory();
        TestPopulator.populateProjects(emf);
        app = ApplicationConfig.startServer(7080, emf);

        // register callback
        context.getRoot().getStore(GLOBAL).put("ApiTest", this);

        RestAssured.baseURI = "http://localhost:7080";

        JWT_TOKEN = JWTUtil.createToken(1L, "test@example.org", Role.PROJECT_MANAGER);
    }

    @Override
    public void close()
    {
        postgres.stop();
        emf.close();
        app.stop();
    }
}
