package org.pmf.eao;

import org.pmf.entity.User;

import javax.ejb.Local;

@Local
public interface UserEao extends Eao {

    public User findUserByUsername(String username, User.Role role);

    public User findUserByUid(int uid, User.Role role);

}
