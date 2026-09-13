package app.persistence.interfaces.specific;

import app.persistence.interfaces.generic.ICreateDAO;
import app.persistence.interfaces.generic.IDeleteDAO;
import app.persistence.interfaces.generic.IUpdateDAO;
import app.enums.UserRole;
import app.persistence.interfaces.readers.IUserReader;
import app.entities.User;

import java.util.Set;

public interface IUserDAO extends IUserReader, ICreateDAO<User>, IUpdateDAO<User>, IDeleteDAO<User>
{
    Set<User> findByRole(UserRole role);
}
