package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DownloadComponent{
    public static DownloadTask CreateTask(ArrayList<String> URL, File Directory, DownloadTaskCallback Callback){
        ArrayList<URL> URL_list = new ArrayList<>();
        for (String url_str : URL){
            try {
                URL_list.add(new URL(url_str));
            }catch (MalformedURLException ignore) { }
        }

        // Create task
        return new DownloadTask(0, URL_list, Directory, new DownloadStatus(0, -1, 1, URL_list.size()), Callback);
    }

    public static DownloadAsyncTaskContainer RunTask(DownloadTask Task){
        return new DownloadAsyncTaskContainer(new DownloadAsyncTask(Task));
    }
}