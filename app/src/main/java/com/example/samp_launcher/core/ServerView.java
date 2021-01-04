package com.example.samp_launcher.core;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.samp_launcher.R;

public class ServerView extends LinearLayout {
    private View RootView;
    private ServerConfig Config;

    private boolean Show_IP_Port;
    private boolean HideInfoWhenServerStatusError;

    public ServerView(Context context) {
        super(context);
        this.Init(context);
    }
    public ServerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.Init(context);
    }
    public ServerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.Init(context);
    }

    public void SetServer(ServerConfig Config){
        TextView statusText = this.RootView.findViewById(R.id.server_status);

        TextView nameText = this.RootView.findViewById(R.id.server_name);
        TextView webUrlText = this.RootView.findViewById(R.id.server_weburl);
        TextView ipPortText = this.RootView.findViewById(R.id.server_ip_port);
        TextView versionText = this.RootView.findViewById(R.id.server_version);

        TextView playersText = this.RootView.findViewById(R.id.server_players);

        TextView timeText = this.RootView.findViewById(R.id.server_time);
        TextView mapText = this.RootView.findViewById(R.id.server_map);
        TextView modeText = this.RootView.findViewById(R.id.server_mode);
        TextView languageText = this.RootView.findViewById(R.id.server_language);

        TextView passwordText = this.RootView.findViewById(R.id.server_password);

        Resources resources = this.RootView.getResources();

        // Set status text
        if (Config.Status == ServerStatus.PENDING){
            statusText.setText(resources.getString(R.string.server_status_pending));
        }else if (Config.Status == ServerStatus.ONLINE){
            statusText.setText(resources.getString(R.string.server_status_online));
        }else if (Config.Status == ServerStatus.OFFLINE){
            statusText.setText(resources.getString(R.string.server_status_offline));
        }else if (Config.Status == ServerStatus.NOT_FOUND){
            statusText.setText(resources.getString(R.string.server_status_not_found));
        }else if (Config.Status == ServerStatus.FAILED_TO_FETCH){
            statusText.setText(resources.getString(R.string.server_status_failed_to_fetch));
        }else if (Config.Status == ServerStatus.INCORRECT_IP){
            statusText.setText(resources.getString(R.string.server_status_incorrect_ip));
        }

        // Set status color
        if (ServerConfig.IsStatusError(Config.Status)){
            statusText.setTextColor(resources.getColor(R.color.colorError));
        }else if (ServerConfig.IsStatusNone(Config.Status)){
            statusText.setTextColor(resources.getColor(R.color.colorNone));
        }else if (ServerConfig.IsStatusOk(Config.Status)){
            statusText.setTextColor(resources.getColor(R.color.colorOk));
        }

        // Set server name label
        nameText.setText(CheckProperty(Config.Name, resources));

        // Set web-url ( server site ) label
        webUrlText.setText(CheckProperty(Config.WebURL, resources));

        // Set version label
        versionText.setText(CheckProperty(Config.Version, resources));

        // Set ip/port string
        ipPortText.setText(String.format(resources.getString(R.string.server_port_ip), Config.IP, Config.Port));

        // Set online players count label
        playersText.setText(String.format(resources.getString(R.string.server_players), Config.OnlinePlayers, Config.MaxPlayers));

        // Set time label
        timeText.setText(String.format(resources.getString(R.string.server_time), CheckProperty(Config.Time, resources)));

        // Set map label
        mapText.setText(String.format(resources.getString(R.string.server_map), CheckProperty(Config.Map, resources)));

        // Set mode label
        modeText.setText(String.format(resources.getString(R.string.server_mode), CheckProperty(Config.Mode, resources)));

        // Set language label
        languageText.setText(String.format(resources.getString(R.string.server_language), CheckProperty(Config.Language, resources)));

        // Set password label
        passwordText.setText(String.format(resources.getString(R.string.server_password), CheckProperty(Config.Password, resources)));

        this.Config = Config;

        // Update rules
        this.UpdateRule_HideInfoWhenServerStatusError();
    }

    public ServerConfig GetServerConfig(){
        return this.Config;
    }

    public void SetShowIpPortState(boolean IsEnabled, boolean UpdateAnyway){
        if (this.Show_IP_Port != IsEnabled || UpdateAnyway){
            TextView ipPortText = this.RootView.findViewById(R.id.server_ip_port);

            if (!IsEnabled){
                ipPortText.setVisibility(INVISIBLE); // Hide
            }else{
                ipPortText.setVisibility(VISIBLE); // Show
            }

            this.Show_IP_Port = IsEnabled;
        }
    }
    public boolean GetShowIpPortState(){
        return this.Show_IP_Port;
    }

    public void SetHideInfoWhenServerStatusErrorState(boolean IsEnabled, boolean UpdateAnyway){
        if (this.HideInfoWhenServerStatusError != IsEnabled || UpdateAnyway){
            this.HideInfoWhenServerStatusError = IsEnabled;

            this.UpdateRule_HideInfoWhenServerStatusError();
        }
    }
    public boolean GetHideInfoWhenServerStatusErrorState(){
        return this.HideInfoWhenServerStatusError;
    }

    private void Init(Context context){
        this.RootView = inflate(context, R.layout.server_view, this);
        
        this.SetShowIpPortState(false, true); // Hide ip:port label
        this.SetHideInfoWhenServerStatusErrorState(true, true); // Hide info labels when error status
    }

    private void UpdateRule_HideInfoWhenServerStatusError(){
        LinearLayout InfoLayout = this.RootView.findViewById(R.id.info_layout);

        if (this.HideInfoWhenServerStatusError && this.Config != null && ServerConfig.IsStatusError(this.Config.Status)){
            InfoLayout.setVisibility(INVISIBLE);
        }else{
            InfoLayout.setVisibility(VISIBLE);
        }
    }

    private static String CheckProperty(String Property, Resources Res){
        if (Property.equals("")) Property = Res.getString(R.string.none_string);
        return Property;
    }
}
