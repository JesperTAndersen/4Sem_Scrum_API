package app.config;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.persistence.testutils.TestPopulator;
import app.security.domain.Role;
import app.utils.JWTUtil;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManagerFactory;

/**
 * Starts one application server for all endpoint tests and reseeds the database before each test class,
 * so a class never depends on data left behind by another class.
 */
public class ApiTestExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final long UNKNOWN_ID = 999999L;

    private static EntityManagerFactory emf;
    private static Javalin app;
    private static boolean started;

    @Override
    public void beforeAll(ExtensionContext context)
    {
        synchronized (ApiTestExtension.class)
        {
            if (!started)
            {
                emf = HibernateTestConfig.getEntityManagerFactory();
                app = ApplicationConfig.startServer(7080, emf);

                // The root store closes this resource once, after every endpoint test class.
                context.getRoot().getStore(GLOBAL).put("ApiTestExtension", this);

                RestAssured.baseURI = "http://localhost:7080/api/v1";
                started = true;
            }

            // Seeds users 1 (PROJECT_MANAGER), 2 and 3 (EMPLOYEE) and projects 1, 2 and 3.
            TestPopulator.populateProjects(emf);
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

    public static String managerToken()
    {
        return JWTUtil.createToken(1L, "alice@example.com", Role.PROJECT_MANAGER);
    }

    public static String employeeToken()
    {
        return JWTUtil.createToken(2L, "bob@example.com", Role.EMPLOYEE);
    }

    /**
     * Resolves the token column of an access CSV: {@code NONE}, {@code EMPLOYEE} or {@code PROJECT_MANAGER}.
     */
    public static String tokenFor(String token)
    {
        return switch (token)
        {
            case "NONE" -> null;
            case "EMPLOYEE" -> employeeToken();
            case "PROJECT_MANAGER" -> managerToken();
            default -> throw new IllegalArgumentException("Unknown token: " + token);
        };
    }

    public static RequestSpecification givenToken(String token)
    {
        RequestSpecification request = given().header("Content-Type", "application/json");
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    public static RequestSpecification givenManager()
    {
        return givenToken(managerToken());
    }

    public static JsonNode readJson(String json)
    {
        try
        {
            return OBJECT_MAPPER.readTree(json);
        }
        catch (Exception e)
        {
            throw new AssertionError("Response was not valid JSON: " + json, e);
        }
    }

    /**
     * Posts a valid body as a project manager and returns the ID of the created resource.
     */
    public static long createAsManager(String path, String json)
    {
        String response = givenManager().body(json)
                .when().post(path)
                .then().statusCode(201)
                .extract().asString();
        return readJson(response).get("id").asLong();
    }
}
