package app.persistence.daos;

import app.config.HibernateTestConfig;
import app.entities.User;
import app.enums.Role;
import app.persistence.implementations.UserDAO;
import app.persistence.testutils.TestPopulator;
import app.utils.PasswordUtil;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDAOTest
{
    private final EntityManagerFactory emf = HibernateTestConfig.getEntityManagerFactory();
    private UserDAO userDAO;
    private Map<String, User> seeded;

    @BeforeEach
    void setUp()
    {
        seeded = TestPopulator.populateUsers(emf);
        userDAO = new UserDAO(emf);
    }

    @AfterAll
    void tearDown()
    {
        emf.close();
    }

    @Test
    @DisplayName("Create - should persist a user and generate an ID")
    void create()
    {
        User created = userDAO.create(new User("Test", "User", "test@example.com",
                PasswordUtil.hashPassword("Password123", 4)));

        assertThat(created.getId(), notNullValue());
        User fetched = userDAO.get(created.getId());
        assertThat(fetched.getEmail(), is("test@example.com"));
        assertThat(fetched.getRole(), is(Role.EMPLOYEE));
        assertTrue(fetched.verifyPassword("Password123"));
    }

    @Test
    @DisplayName("Read - should retrieve users and all persisted users")
    void readAndGetAll()
    {
        User user = seeded.get("user1");

        assertThat(userDAO.get(user.getId()).getEmail(), is("alice@example.com"));
        assertThat(userDAO.getAll(), hasSize(3));
    }

    @Test
    @DisplayName("Read - should find users by email, role, and existence")
    void findByEmailRoleAndExistence()
    {
        User employee = seeded.get("user2");
        Set<User> employees = userDAO.findByRole(Role.EMPLOYEE);

        assertThat(userDAO.findByEmail("bob@example.com").orElseThrow().getId(), is(employee.getId()));
        assertThat(userDAO.findByEmail("missing@example.com"), is(Optional.empty()));
        assertThat(employees, hasSize(2));
        assertThat(employees, hasItems(seeded.get("user2"), seeded.get("user3")));
        assertTrue(userDAO.existsByEmail("alice@example.com"));
        assertFalse(userDAO.existsByEmail("missing@example.com"));
    }

    @Test
    @DisplayName("Update - should persist changed user profile, role, and email")
    void update()
    {
        User user = seeded.get("user1");
        user.update("Updated", "Manager");
        user.changeRole(Role.EMPLOYEE);
        user.changeEmail("updated@example.com");

        userDAO.update(user);

        User fetched = userDAO.get(user.getId());
        assertThat(fetched.getFirstName(), is("Updated"));
        assertThat(fetched.getLastName(), is("Manager"));
        assertThat(fetched.getEmail(), is("updated@example.com"));
        assertThat(fetched.getRole(), is(Role.EMPLOYEE));
    }

    @Test
    @DisplayName("Delete - should remove an existing user")
    void delete()
    {
        Long id = seeded.get("user3").getId();

        assertTrue(userDAO.delete(id));
        assertThrows(EntityNotFoundException.class, () -> userDAO.get(id));
    }

    @Test
    @DisplayName("Read, update, and delete - should reject invalid IDs and missing users")
    void invalidIdsAndMissingUsers()
    {
        User withoutId = new User("No", "ID", "noid@example.com", PasswordUtil.hashPassword("Password123", 4));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.get(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.delete(0L)),
                () -> assertThrows(EntityNotFoundException.class, () -> userDAO.get(999L)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.create(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.update(withoutId)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.findByEmail(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.findByRole(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> userDAO.existsByEmail(null))
        );
    }
}
