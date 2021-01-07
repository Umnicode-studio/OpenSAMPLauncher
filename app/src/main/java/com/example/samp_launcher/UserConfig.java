package com.example.samp_launcher;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.samp_launcher.core.ServerConfig;
import com.google.gson.Gson;

import java.util.ArrayList;

public class UserConfig {
    private Context _Context;
    private String ConfigName;

    public boolean IsSetup = false;
    public String Nickname = "";
    public int PingTimeout = 0;

    public ArrayList<ServerConfig> ServerList = new ArrayList<>();

    UserConfig(Context _Context, String CfgName){
        this._Context = _Context;
        this.ConfigName = CfgName;

        this.Load(this.ConfigName);
    }

    public void Reload(){
        this.Load(this.ConfigName);
    }
    public void Load(String ConfigName){
        SharedPreferences Prefs = _Context.getSharedPreferences(ConfigName, Context.MODE_PRIVATE);

        this.IsSetup = Prefs.getBoolean("IsSetup", false);
        this.Nickname = Prefs.getString("Nickname", "");
        this.PingTimeout = Prefs.getInt("PingTimeout", 3000);

        // Load list
        this.ServerList.clear();

        Gson gson = new Gson();
        try {
            this.ServerList = gson.fromJson(Prefs.getString("Servers", "[]"), this.ServerList.getClass());
        }catch (Exception ignore){ }
    }

    public void Save(){
        this.SaveAs(this.ConfigName);
    }
    public void SaveAs(String ConfigName){
        SharedPreferences Prefs = _Context.getSharedPreferences(ConfigName, Context.MODE_PRIVATE);
        SharedPreferences.Editor PrefsEditor = Prefs.edit();

        PrefsEditor.putBoolean("IsSetup", this.IsSetup);
        PrefsEditor.putString("Nickname", this.Nickname);
        PrefsEditor.putInt("PingTimeout", this.PingTimeout);

        // Save list of servers
        Gson gson = new Gson();
        String JsonStr = gson.toJson(this.ServerList);

        PrefsEditor.putString("Servers", JsonStr);

        PrefsEditor.apply();
    }
}
