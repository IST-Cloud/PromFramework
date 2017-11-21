package org.pmf.online;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.*;
import org.pmf.common.Common;
import org.pmf.eao.PluginEao;
import org.pmf.entity.Invoke;
import org.pmf.entity.Plugin;
import org.pmf.plugin.service.PluginService;

import javax.ejb.EJB;
import javax.servlet.GenericServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by huangtao on 2017/4/18.
 */
public class OnlineProcessMiner {

    private PluginEao pluginEao;

    private int port;




    private Queue<XTrace> waitingQueue;
    private boolean isRcvOver = false;
    private Map<String, String> minerParam;
    private GenericServlet servletContext;

    private String key;
    private String apiKey;

    private int basketSize = 50;
    private int miningInterval = 5;
    private Queue<XTrace> basket;

    private JSONObject miningResult = new JSONObject();

    public OnlineProcessMiner(int port, Map<String, String> params, GenericServlet servlet, PluginEao pluginEao) {
        minerParam = params;
        this.port = port;
        this.key = params.get(Common.KEY_KEY);
        this.apiKey = params.get(Common.KEY_APIKEY);
        waitingQueue = new LinkedBlockingQueue<>();
        basket = new LinkedBlockingQueue<>();
        minerParam = new HashMap<>();
        this.servletContext = servlet;
        this.pluginEao = pluginEao;
    }

    public void startRcvLog(String logUrl) {
        Thread thread = new Thread(() -> {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(port);
                Socket socket = ss.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                while (true) {
                    socket.setKeepAlive(true);

                    String rcvStr = reader.readLine();
                    if (!rcvStr.isEmpty()) {
                        if (rcvStr.equals("over")) {
                            System.out.println("Rcv over");
                            isRcvOver = true;
                            break;
                        }
                        JSONObject rcvJsn = JSONObject.fromObject(rcvStr);
                        if(!rcvJsn.getString(Common.KEY_KEY).equals(key)){
                            JSONObject jsn = new JSONObject();
                            jsn.put("status", "ERROR");
                            writer.println(jsn.toString());
                            writer.flush();
                            continue;
                        }

                        waitingQueue.offer(convertJsnArrayToXTrace(rcvJsn.getJSONArray(Common.KEY_LOG)));
//                        System.out.println("Rcv log: " + rcvStr);
                        if (!miningThread.isAlive()) {
                            miningThread.start();
                        }

                        JSONObject jsn = new JSONObject();
                        jsn.put("status", "OK");
                        writer.println(jsn.toString());
                        writer.flush();
                    }
                    Thread.sleep(10);
                }
                reader.close();
                writer.close();
                socket.close();
                ss.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                if(ss!=null){
                    try {
                        ss.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        thread.start();

        try {
            // notice log sender to send logs
            JSONObject connJsn = new JSONObject();
            connJsn.put(Common.KEY_KEY, key);
            connJsn.put(Common.KEY_URL, InetAddress.getLocalHost().getHostAddress());
            connJsn.put(Common.KEY_PORT, port);

            if(!logUrl.startsWith("http://")){
                logUrl = "http://" + logUrl;
            }
            URL url = new URL(logUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            PrintWriter connWriter = new PrintWriter(httpURLConnection.getOutputStream());
            connWriter.write("param=" + connJsn.toString());
            connWriter.flush();
            BufferedReader connReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            try {
                JSONObject connResultJsn = JSONObject.fromObject(connReader.readLine());
                if (!connResultJsn.getString(Common.KEY_STATUS).equals("OK")) {
                    System.out.println("Log sender error");
                    thread.stop();
                }
            } catch (Exception e) {
                System.out.println("Log sender error");
                thread.stop();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private Thread miningThread = new Thread(() -> {
        while (true) {
            if (waitingQueue.size() >= miningInterval) {
                if (basket.size() >= basketSize) {
                    for (int i = 0; i < miningInterval; i++) {
                        basket.poll();
                    }
                }
                for (int i = 0; i < miningInterval; i++) {
                    basket.offer(waitingQueue.poll());
                }
                if (basket.size() >= basketSize) {
                    miningResult = doMining();
                }
            } else if(isRcvOver){
                System.out.println("Mining over");
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    private JSONObject doMining(){
        try {
            Plugin plugin = pluginEao.findAvailablePluginByApiKey(apiKey);
            if (plugin == null) {
                return null;
            }
            XLog log = getBudgetLog();
            String pluginPath = servletContext.getServletContext().getRealPath("/WEB-INF/plugin");
            ClassLoader cl = new URLClassLoader(new URL[]{new URL("file:///" + pluginPath + "/" + plugin.getJarName())}, this.getClass().getClassLoader());

            Class<?> c = Class.forName(plugin.getServiceClass(), true, cl);
            Class<? extends PluginService> pluginClass = c.asSubclass(PluginService.class);
            PluginService service = pluginClass.newInstance();
            JSONObject json = service.doPluginService(log, minerParam);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            json.put("cal_time", df.format(new Date()));

            // update plugin
            plugin.addInvoke(new Invoke(new Date()));
            pluginEao.update(plugin);
            return json;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private XTrace convertJsnArrayToXTrace(JSONArray jsnArray) {
        XTrace trace = new XTraceImpl(new XAttributeMapImpl());
        for (int i = 0; i < jsnArray.size(); i++) {
            XAttributeMap attributeMap = new XAttributeMapImpl();
            attributeMap.put("concept:name", new XAttributeLiteralImpl("concept:name", jsnArray.getJSONObject(i).getString("EventName")));
            XEvent event = new XEventImpl(attributeMap);
            trace.add(event);
        }
        return trace;
    }

    private XLog getBudgetLog(){
        XLog log = new XLogImpl(new XAttributeMapImpl());
        for(XTrace trace : basket){
            log.add(trace);
        }
        return log;
    }

    public JSONObject getMiningResult(){
        return miningResult;
    }

}
