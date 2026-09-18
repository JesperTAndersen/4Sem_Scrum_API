package app.persistence.testdoubles;

import app.security.domain.Role;
import app.user.data.IUserDAO;
import app.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InMemoryUserDAO implements IUserDAO
{
    private final User user;

    public InMemoryUserDAO(User user)
    {
        this.user = user;
    }

    @Override public User get(Long id) { return user; }
    @Override public List<User> getAll() { return List.of(user); }
    @Override public User create(User value) { return value; }
    @Override public User update(User value) { return value; }
    @Override public boolean delete(Long id) { return true; }
    @Override public boolean existsByEmail(String email) { return user != null && user.getEmail().equals(email); }
    @Override public Optional<User> findByEmail(String email) { return existsByEmail(email) ? Optional.of(user) : Optional.empty(); }
    @Override public Set<User> findByRole(Role role) { return Set.of(user); }
}
