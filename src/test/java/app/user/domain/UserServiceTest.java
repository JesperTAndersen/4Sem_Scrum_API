package app.user.domain;

import app.persistence.testdoubles.InMemoryUserDAO;
import app.security.domain.Role;
import app.security.presentation.dto.AuthenticatedUser;
import app.user.presentation.dto.ChangeUserPasswordDTO;
import app.user.presentation.dto.EmailUpdateDTO;
import app.user.presentation.dto.UpdateUserDTO;
import app.user.presentation.dto.UserDTO;
import app.user.presentation.dto.UserRoleUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest
{
    private InMemoryUserDAO userDAO;
    private UserService userService;

    /**
     * Seeds user 1 (ada@example.com) and user 2 (bob@example.com), both with the password {@code Password1!}.
     */
    @BeforeEach
    void setUp()
    {
        userDAO = new InMemoryUserDAO();
        userDAO.addUser("Ada", "Lovelace", "ada@example.com", "Password1!");
        userDAO.addUser("Bob", "Builder", "bob@example.com", "Password1!");
        userService = new UserService(userDAO);
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/user/user-service/update-profile.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update profile - only the own existing profile, with names of at least 2 characters")
    void updateValidatesProfile(Long authUserId, Long targetUserId, String firstName, String lastName,
                                String expected, String reason)
    {
        AuthenticatedUser authUser = authenticated(authUserId);
        UpdateUserDTO dto = new UpdateUserDTO(firstName, lastName);

        if (expected.equals("ACCEPT"))
        {
            userService.update(authUser, targetUserId, dto);
            UserDTO stored = userService.findById(targetUserId);
            assertThat(stored.firstName(), is(firstName.trim()));
            assertThat(stored.lastName(), is(lastName.trim()));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> userService.update(authUser, targetUserId, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/user/user-service/change-email.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change email - only the own existing account, with a valid and unique email")
    void changeEmailValidatesEmail(Long authUserId, Long targetUserId, String email, String expected, String reason)
    {
        AuthenticatedUser authUser = authenticated(authUserId);
        EmailUpdateDTO dto = new EmailUpdateDTO(email);

        if (expected.equals("ACCEPT"))
        {
            userService.changeEmail(authUser, targetUserId, dto);
            assertThat(userService.findById(targetUserId).email(), is(email));
        }
        else
        {
            Exception exception = assertThrows(Exception.class,
                    () -> userService.changeEmail(authUser, targetUserId, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/user/user-service/change-password.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change password - only the own existing account, with the current password and a strong new one")
    void changePasswordValidatesPasswords(Long authUserId, Long targetUserId, String currentPassword,
                                          String newPassword, String expected, String reason)
    {
        AuthenticatedUser authUser = authenticated(authUserId);
        ChangeUserPasswordDTO dto = new ChangeUserPasswordDTO(currentPassword, newPassword);

        if (expected.equals("ACCEPT"))
        {
            userService.changePassword(authUser, targetUserId, dto);
            assertThat(userDAO.get(targetUserId).verifyPassword(newPassword), is(true));
        }
        else
        {
            Exception exception = assertThrows(Exception.class,
                    () -> userService.changePassword(authUser, targetUserId, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/user/user-service/change-role.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change role - requires an existing user and a role")
    void changeRoleValidatesRole(Long targetUserId, Role role, String expected, String reason)
    {
        UserRoleUpdateDTO dto = new UserRoleUpdateDTO(role);

        if (expected.equals("ACCEPT"))
        {
            userService.changeRole(targetUserId, dto);
            assertThat(userService.findById(targetUserId).role(), is(role));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> userService.changeRole(targetUserId, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/user/user-service/delete.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - only another existing user")
    void deleteValidatesTarget(Long authUserId, Long targetUserId, String expected, String reason)
    {
        AuthenticatedUser authUser = authenticated(authUserId);

        if (expected.equals("ACCEPT"))
        {
            userService.delete(authUser, targetUserId);
            Exception exception = assertThrows(Exception.class, () -> userService.findById(targetUserId));
            assertThat(exception.getClass().getSimpleName(), is("NotFoundException"));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> userService.delete(authUser, targetUserId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/user/user-service/find-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Find by id - requires an existing user")
    void findByIdRequiresExistingUser(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(userService.findById(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> userService.findById(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    private AuthenticatedUser authenticated(Long userId)
    {
        return new AuthenticatedUser(userId, "user" + userId + "@example.com", Role.EMPLOYEE);
    }
}
