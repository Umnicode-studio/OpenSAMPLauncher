package com.example.samp_launcher;

import android.app.Application;
import android.content.Context;

import com.example.samp_launcher.core.SAMP.SAMPInstaller;

public class LauncherApplication extends Application {
    private static Context _Context;
    public UserConfig userConfig;
    public SAMPInstaller Installer;

    public void onCreate(){
        super.onCreate();

        _Context = this.getApplicationContext();
        this.userConfig = new UserConfig(this.getApplicationContext(),
                                         this.getApplicationContext().getString(R.string.user_config_name));
        this.Installer = new SAMPInstaller();
    }

    public static Context getAppContext(){
        return _Context;
    }
}
