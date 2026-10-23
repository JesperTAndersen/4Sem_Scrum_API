package app.user.data;

import app.security.domain.Role;
import app.shared.data.ICreateDAO;
import app.shared.data.IDeleteDAO;
import app.shared.data.IUpdateDAO;
import app.user.data.IUserReader;
import app.user.domain.User;

import java.util.Set;

public interface IUserDAO extends IUserReader, ICreateDAO<User>, IUpdateDAO<User>, IDeleteDAO<User>
{
    Set<User> findByRole(Role role);
}
