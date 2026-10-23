package app.user.data;

import app.shared.data.IReadDAO;
import app.user.domain.User;

import java.util.Optional;

public interface IUserReader extends IReadDAO<User>
{
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
