package app.security.domain;

import app.persistence.testdoubles.InMemoryUserDAO;
import app.security.presentation.dto.AuthenticatedUser;
import app.security.presentation.dto.LoginRequestDTO;
import app.user.domain.User;
import app.user.presentation.dto.CreateUserRequestDTO;
import app.utils.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityServiceTest
{
    private InMemoryUserDAO userDAO;
    private SecurityService securityService;
    private User manager;

    @BeforeEach
    void setUp()
    {
        userDAO = new InMemoryUserDAO();
        manager = userDAO.addUser("Project", "Manager", "manager@example.com", "Password1!");
        manager.changeRole(Role.PROJECT_MANAGER);
        securityService = new SecurityService(userDAO);
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/security/security-service/register.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Register - validates names, a unique email and a strong password")
    void registerValidatesInput(String firstName, String lastName, String email, String password,
                                String expected, String reason)
    {
        CreateUserRequestDTO request = new CreateUserRequestDTO(firstName, lastName, email, password);

        if (expected.equals("ACCEPT"))
        {
            AuthenticatedUser registered = securityService.register(request);
            assertThat(registered.role(), is(Role.EMPLOYEE));
            assertThat(registered.email(), is(email.trim().toLowerCase(Locale.ROOT)));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> securityService.register(request));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/security/security-service/login.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Login - returns a token for the user only for valid credentials")
    void loginValidatesCredentials(String email, String password, String expected, String reason)
    {
        LoginRequestDTO request = new LoginRequestDTO(email, password);

        if (expected.equals("ACCEPT"))
        {
            AuthenticatedUser authenticatedUser = JWTUtil.parseToken(securityService.login(request));
            assertThat(authenticatedUser.id(), is(manager.getId()));
            assertThat(authenticatedUser.role(), is(Role.PROJECT_MANAGER));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> securityService.login(request));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }
}
