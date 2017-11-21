package org.pmf.servlet;

import net.sf.json.JSONObject;
import org.pmf.common.Common;
import org.pmf.eao.UserEao;
import org.pmf.entity.User;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by huangtao on 2017/4/11.
 * <p>
 * login and register
 */
@WebServlet("/UserServlet")
public class UserServlet extends HttpServlet {

    @EJB
    private UserEao userEao;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String opstr = request.getParameter("op");
        int op = Integer.parseInt(opstr);
        switch(op){
            case 0:
                this.login(request, response);
                break;
            case 1:
                this.register(request, response);
                break;
            default:
                break;
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response){
        JSONObject jsn = JSONObject.fromObject(request.getParameter("param"));

        String username = jsn.getString(Common.KEY_USERNAME);
        String password = jsn.getString(Common.KEY_PASSWORD);

        User user = userEao.findUserByUsername(username, null);
        if(user == null){
            Common.postErrorMsg("Username is invalid.", response);
            return;
        }

        if(!password.equals(user.getPassword())){
            Common.postErrorMsg("Password is invalid.", response);
            return;
        }

        JSONObject resultJsn = new JSONObject();
        resultJsn.put(Common.KEY_UID, user.getUid());
        resultJsn.put(Common.KEY_USERNAME, user.getUsername());
        Common.postSuccessMsg(resultJsn, response);
    }

    private void register(HttpServletRequest request, HttpServletResponse response){
        JSONObject jsn = JSONObject.fromObject(request.getParameter("param"));

        String username = jsn.getString(Common.KEY_USERNAME);
        if (userEao.findUserByUsername(username, null) != null) {
            Common.postErrorMsg("This username has been registered, please try another one.", response);
            return;
        }

        User.Role role;
        try {
            role = User.Role.valueOf(jsn.getString(Common.KEY_ROLE));
        } catch (Exception e){
            Common.postErrorMsg("Parameter \"role\" is invalid.", response);
            return;
        }

        String password = jsn.getString(Common.KEY_PASSWORD);

        User user = new User(username, password, role);
        userEao.save(user);
        Common.postSuccessMsg(new JSONObject(), response);
    }

}
