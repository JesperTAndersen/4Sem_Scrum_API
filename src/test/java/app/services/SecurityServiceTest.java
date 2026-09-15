package app.services;

import app.dtos.security.AuthenticatedUser;
import app.dtos.security.LoginRequestDTO;
import app.entities.User;
import app.enums.Role;
import app.exceptions.UnauthorizedException;
import app.persistence.interfaces.specific.IUserDAO;
import app.services.implementations.SecurityService;
import app.utils.JWTUtil;
import app.utils.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityServiceTest
{
    @Test
    @DisplayName("Login - should authenticate valid credentials and return a token for the user")
    void validCredentialsAuthenticate()
    {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        User user = userDAO.addUser("manager@example.com", "Password123!");
        user.changeRole(Role.PROJECT_MANAGER);
        SecurityService securityService = new SecurityService(userDAO);

        String token = securityService.login(new LoginRequestDTO("manager@example.com", "Password123!"));
        AuthenticatedUser authenticatedUser = JWTUtil.parseToken(token);

        assertThat(token, not(isEmptyOrNullString()));
        assertThat(authenticatedUser.id(), is(user.getId()));
        assertThat(authenticatedUser.email(), is(user.getEmail()));
        assertThat(authenticatedUser.role(), is(Role.PROJECT_MANAGER));
    }

    @Test
    @DisplayName("Login - should reject unknown users and incorrect passwords with the same authentication error")
    void invalidCredentialsFail()
    {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        userDAO.addUser("manager@example.com", "Password123!");
        SecurityService securityService = new SecurityService(userDAO);

        UnauthorizedException unknownUser = assertThrows(UnauthorizedException.class,
                () -> securityService.login(new LoginRequestDTO("missing@example.com", "Password123!")));
        UnauthorizedException wrongPassword = assertThrows(UnauthorizedException.class,
                () -> securityService.login(new LoginRequestDTO("manager@example.com", "WrongPassword1!")));

        assertThat(unknownUser.getMessage(), is("Invalid credentials"));
        assertThat(wrongPassword.getMessage(), is("Invalid credentials"));
    }

    private static final class InMemoryUserDAO implements IUserDAO
    {
        private final Map<Long, User> users = new LinkedHashMap<>();
        private long nextId = 1L;

        private User addUser(String email, String password)
        {
            User user = new User("Test", "User", email, PasswordUtil.hashPassword(password, 4));
            setId(user, nextId++);
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public User create(User user)
        {
            setId(user, nextId++);
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public User get(Long id)
        {
            return users.get(id);
        }

        @Override
        public List<User> getAll()
        {
            return List.copyOf(users.values());
        }

        @Override
        public User update(User user)
        {
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public boolean delete(Long id)
        {
            return users.remove(id) != null;
        }

        @Override
        public Optional<User> findByEmail(String email)
        {
            return users.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email)
        {
            return findByEmail(email).isPresent();
        }

        @Override
        public Set<User> findByRole(Role role)
        {
            return users.values().stream()
                    .filter(user -> user.getRole() == role)
                    .collect(java.util.stream.Collectors.toSet());
        }

        private static void setId(User user, Long id)
        {
            try
            {
                Field field = User.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(user, id);
            }
            catch (ReflectiveOperationException e)
            {
                throw new AssertionError("Could not assign test user ID", e);
            }
        }
    }
}
