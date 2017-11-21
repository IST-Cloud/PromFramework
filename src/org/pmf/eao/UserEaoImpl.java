package org.pmf.eao;

import org.pmf.entity.Plugin;
import org.pmf.entity.User;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.util.List;

/**
 * Session Bean implementation class UserEaoImpl
 */
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UserEaoImpl extends EaoImpl implements UserEao {

    @Override
    public User findUserByUsername(String username, User.Role role) {
        List<User> userList;
        if (role != null) {
            userList = getResultList(User.class, "where o.username = ?1 and o.role = ?2", null, username, role);
        } else {
            userList = getResultList(User.class, "where o.username = ?1", null, username);
        }
        if (userList != null && userList.size() > 0) {
            return userList.get(0);
        }
        return null;
    }

    @Override
    public User findUserByUid(int uid, User.Role role) {
        List<User> userList;
        if (role != null) {
            userList = getResultList(User.class, "where o.uid = ?1 and o.role = ?2", null, uid, role);
        } else {
            userList = getResultList(User.class, "where o.uid = ?1", null, uid);
        }
        if (userList != null && userList.size() > 0) {
            return userList.get(0);
        }
        return null;
    }
}
