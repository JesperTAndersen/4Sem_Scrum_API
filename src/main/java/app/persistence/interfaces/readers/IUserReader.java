package app.persistence.interfaces.readers;

import app.persistence.interfaces.generic.IReadDAO;
import app.entities.User;

import java.util.Optional;

public interface IUserReader extends IReadDAO<User>
{
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
