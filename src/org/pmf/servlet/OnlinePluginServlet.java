package org.pmf.servlet;

import net.sf.json.JSONObject;
import org.pmf.common.Common;
import org.pmf.eao.PluginEao;
import org.pmf.entity.Plugin;
import org.pmf.online.OnlineProcessMiner;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangtao on 2017/4/18.
 */
@WebServlet("/OnlinePluginServlet")
public class OnlinePluginServlet extends HttpServlet {

    @EJB
    private PluginEao pluginEao;

    private Map<String, OnlineProcessMiner> onlineProcessMinerMap = new HashMap<>();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        String opstr = request.getParameter("op");
        int op = Integer.parseInt(opstr);
        switch (op) {
            case 0:
                this.fetchMiningResult(request, response);
                break;
            case 1:
                this.initOnlineDiscovery(request, response);
                break;
            default:
                break;
        }
    }

    private void fetchMiningResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = request.getParameter(Common.KEY_KEY);
        if(key == null || key.isEmpty()){
            Common.postErrorMsg("Key can not be empty.", response);
            return;
        }

        JSONObject jsn = onlineProcessMinerMap.get(key).getMiningResult();
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        jsn.put("req_time", df.format(new Date()));
        Common.postSuccessMsg(jsn, response);
    }

    private void initOnlineDiscovery(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Common.postSuccessMsg(new JSONObject(), response);

        int port = Common.genPort();
        System.out.println("Accept online mining request, key: " + request.getParameter(Common.KEY_KEY) + " Generate port: " + port);

        Map<String, String> params = new HashMap<>();
        for (String key : request.getParameterMap().keySet()) {
            System.out.println(key + " : " + request.getParameter(key));
            params.put(key, request.getParameter(key));
        }

        OnlineProcessMiner miner = new OnlineProcessMiner(port, params, this, pluginEao);
        onlineProcessMinerMap.put(request.getParameter(Common.KEY_KEY), miner);
        miner.startRcvLog(request.getParameter(Common.KEY_LOG_URL));

//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println(miner.getMiningResult());
//            }
//        }).start();

    }

}
