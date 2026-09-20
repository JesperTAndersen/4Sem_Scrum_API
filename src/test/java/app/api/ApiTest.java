package app.api;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import com.fasterxml.jackson.databind.ObjectMapper;

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
class ApiTest implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
    static String JWT_TOKEN;
    private static EntityManagerFactory emf;
    private static Javalin app;
    private static boolean started;
    static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void beforeAll(ExtensionContext context)
            throws Exception
    {
        synchronized (ApiTest.class)
        {
            if (started)
            {
                return;
            }

            emf = HibernateTestConfig.getEntityManagerFactory();
            TestPopulator.populateProjects(emf);
            app = ApplicationConfig.startServer(7080, emf);

            // The root store closes this resource once, after every API test class.
            context.getRoot().getStore(GLOBAL).put("ApiTest", this);

            RestAssured.baseURI = "http://localhost:7080/api/v1";

            JWT_TOKEN = JWTUtil.createToken(1L, "test@example.org", Role.PROJECT_MANAGER);
            started = true;
        }
    }

    @Override
    public void close()
    {
        if (app != null)
        {
            app.stop();
        }
        if (emf != null && emf.isOpen())
        {
            emf.close();
        }
        started = false;
    }
}
