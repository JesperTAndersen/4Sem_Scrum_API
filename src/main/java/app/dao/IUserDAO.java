package app.dao;

import app.enums.UserRole;
import app.dao.generic.IEntityDAO;
import app.dao.readers.IUserReader;
import app.entities.User;

import java.util.Set;

public interface IUserDAO extends IUserReader, IEntityDAO<User, Long>
{
    Set<User> getAll();

    Set<User> findByRole(UserRole role);
}
