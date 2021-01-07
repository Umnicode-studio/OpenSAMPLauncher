package com.example.samp_launcher.core;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.samp_launcher.core.SAMP.Enums.ServerStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;

public class ServerConfig {
    public String IP = "";
    public int Port = 0;

    public String Name = "";
    public String Password = "";

    public String Version = "";
    public String WebURL = "";

    public String Time = "";

    public int OnlinePlayers = 0;
    public int MaxPlayers = 0;

    public String Mode = "";
    public String Map = "";

    public String Language = "";

    public ServerStatus Status = ServerStatus.NONE;

    public ServerConfig(){

    }
    public ServerConfig(ServerStatus Status){
        this.Status = Status;
    }

    static public boolean IsIPCorrect(String IP){
        if (IP.isEmpty()) return false;

        try{
            String[] Parts = IP.split("\\.");
            if (Parts.length != 4) return false;

            for (String str : Parts){
                int i = Integer.parseInt(str);
                if (i < 0 || i > 255) return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static String SafeJsonGet(String Name, JsonObject Object){
        String Str = Object.get(Name).getAsString();
        if (Str == null) Str = "";
        return Str;
    }
    private static int SafeJsonToInt(String PropName, JsonObject Object){
        try {
            String Str = Object.get(PropName).getAsString();
            if (Str != null){
                return Integer.parseInt(Str);
            }
        } catch (NumberFormatException ignore){
            Log.println(Log.ERROR, "ServerConfig", "NumberFormatException in " + PropName);
        }

        return 0;
    }

    static public void Resolve(String IP, int Port, int PingTimeout, Context context, ServerResolveCallback Callback){
        if (IsIPCorrect(IP)) {
            // Big thanks guys from sacnr.com for their public ip
            String url = String.format(Locale.UK, "http://monitor.sacnr.com/api/?IP=%s&Port=%d&Action=info&Format=JSON", IP, Port);

            RequestQueue queue = Volley.newRequestQueue(context);
            StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    public void onResponse(String response) {
                        if (response == null) {
                            Callback.OnFinish(new ServerConfig(ServerStatus.FAILED_TO_FETCH));
                        }else {
                            if (response.equals("Unknown Server ID")) { // Check for not found error
                                Callback.OnFinish(new ServerConfig(ServerStatus.NOT_FOUND));
                            }else{
                                ServerConfig Config = new ServerConfig();

                                // Parse request as JSON ( we set target format as JSON early )
                                try {
                                    JsonObject Object = JsonParser.parseString(response).getAsJsonObject();

                                    // Get props name from object
                                    Config.IP = IP;
                                    Config.Port = Port;

                                    Config.Name = Object.get("Hostname").getAsString();

                                    String Password = SafeJsonGet("Password", Object);
                                    if (Password.equals("0") || Password.equals("")){
                                        Config.Password = "";
                                    }else{
                                        Config.Password = Password;
                                    }

                                    Config.Version = SafeJsonGet("Version", Object);
                                    Config.WebURL = SafeJsonGet("WebURL", Object);
                                    Config.Time = SafeJsonGet("Time", Object);

                                    // Players count
                                    Config.OnlinePlayers = SafeJsonToInt("Players", Object);
                                    Config.MaxPlayers = SafeJsonToInt("MaxPlayers", Object);

                                    Config.Map = SafeJsonGet("Map", Object);
                                    Config.Mode = SafeJsonGet("Gamemode", Object);
                                    Config.Language = SafeJsonGet("Language", Object);

                                    Config.Status = ServerStatus.PENDING;
                                }catch (JsonParseException ex){
                                    Log.println(Log.ERROR, "ServerConfig", "Error parse - " + response);
                                    Config.Status = ServerStatus.FAILED_TO_FETCH;
                                }

                                Callback.OnFinish(Config); // Finish event

                                // Ping
                                Thread ping = new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            Socket Socket = new Socket();
                                            Socket.connect(new InetSocketAddress(InetAddress.getByName(IP), 80), PingTimeout);

                                            Config.Status = ServerStatus.ONLINE;
                                        } catch (UnknownHostException ex) {
                                            Config.Status = ServerStatus.OFFLINE;
                                        } catch (IOException ex) {
                                            Config.Status = ServerStatus.OFFLINE;
                                        }

                                        // Run on main thread
                                        new Handler(context.getMainLooper()).post(new Runnable () {
                                            public void run () {
                                                Callback.OnPingFinish(Config);
                                            }
                                        });
                                    }
                                });

                                ping.start();
                            }
                        }
                    }}, new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Callback.OnFinish(new ServerConfig(ServerStatus.FAILED_TO_FETCH));
                    }
                }
            );

            queue.add(request);
        }else{
            Callback.OnFinish(new ServerConfig(ServerStatus.INCORRECT_IP)); // Send server with FAILED_TO_FETCH status
        }
    }

    static public boolean IsStatusError(ServerStatus Status){
        return (Status != ServerStatus.ONLINE && Status != ServerStatus.OFFLINE && Status != ServerStatus.PENDING);
    }
    static public boolean IsStatusNone(ServerStatus Status){
        return Status == ServerStatus.PENDING;
    }
    static public boolean IsStatusOk(ServerStatus Status){
        return Status == ServerStatus.ONLINE;
    }
}
