package app.persistence.testdoubles;

import app.exceptions.NotFoundException;
import app.security.domain.Role;
import app.user.data.IUserDAO;
import app.user.domain.User;
import app.utils.PasswordUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mirrors {@code UserDAO}: a missing user raises {@link NotFoundException}, and email lookups are exact matches
 * like the database query.
 */
public class InMemoryUserDAO implements IUserDAO
{
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1L;

    /**
     * Adds a user with a cheap password hash, so tests do not pay for the production hashing cost.
     */
    public User addUser(String firstName, String lastName, String email, String password)
    {
        return create(new User(firstName, lastName, email, PasswordUtil.hashPassword(password, 4)));
    }

    @Override
    public User create(User user)
    {
        EntityIds.assign(user, nextId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User get(Long id)
    {
        User user = users.get(id);
        if (user == null)
        {
            throw new NotFoundException("No user found with id: " + id);
        }
        return user;
    }

    @Override
    public List<User> getAll()
    {
        return List.copyOf(users.values());
    }

    @Override
    public User update(User user)
    {
        get(user.getId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean delete(Long id)
    {
        get(id);
        return users.remove(id) != null;
    }

    @Override
    public Optional<User> findByEmail(String email)
    {
        return users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
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
                .collect(Collectors.toSet());
    }
}