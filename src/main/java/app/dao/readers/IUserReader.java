package app.dao.readers;

import app.dao.generic.IEntityReader;
import app.entities.User;

import java.util.Optional;

public interface IUserReader extends IEntityReader<User, Long>
{
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
