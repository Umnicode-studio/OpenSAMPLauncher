package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class DownloadComponent{
    public static DownloadTask CreateTask(List<String> URL, File Directory, DownloadTaskCallback Callback){
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