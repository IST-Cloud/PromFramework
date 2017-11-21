package org.pmf.common;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by huangtao on 2017/4/11.
 */
public class Common {

    // key
    public static String KEY_USERNAME = "username";
    public static String KEY_PASSWORD = "password";
    public static String KEY_ROLE = "role";
    public static String KEY_UID = "uid";
    public static String KEY_STATUS = "status";

    public static String KEY_APIKEY = "apiKey";
    public static String KEY_KEY = "key";
    public static String KEY_LOG_URL = "log_url";
    public static String KEY_LOG = "log";
    public static String KEY_URL = "url";
    public static String KEY_REQUEST = "request";
    public static String KEY_PORT = "port";

    public static void postErrorMsg(String msg, HttpServletResponse response) {
        try {
            JSONObject json = new JSONObject();
            json.element("status", "ERROR");
            json.element("msg", msg);
            response.setContentType("application/json; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.print(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void postSuccessMsg(JSONObject json, HttpServletResponse response) {
        try {
            json.element("status", "OK");
            response.setContentType("application/json; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.print(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 随机生成一个端口号，注意避免重复
     *
     * @return
     */
    public static int genPort(){
        return 10086;
    }

}
