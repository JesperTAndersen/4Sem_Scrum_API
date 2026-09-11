package app.dao.interfaces.specific;

import app.dao.interfaces.generic.ICreateDAO;
import app.dao.interfaces.generic.IDeleteDAO;
import app.dao.interfaces.generic.IUpdateDAO;
import app.enums.UserRole;
import app.dao.interfaces.readers.IUserReader;
import app.entities.User;

import java.util.Set;

public interface IUserDAO extends IUserReader, ICreateDAO<User>, IUpdateDAO<User>, IDeleteDAO<User>
{
    Set<User> findByRole(UserRole role);
}
