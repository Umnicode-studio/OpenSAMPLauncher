package com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem;

import com.umnicode.samp_launcher.core.SAMP.Components.AsyncTaskContainer;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.Utils;

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
        return new DownloadTask(0, URL_list, Directory, TaskStatus.CreateEmpty(URL_list.size()), Callback);
    }

    public static DownloadTask CreateTaskFromFiles(ArrayList<Integer> Indexes, DownloadTask OriginalTask){
        // Warning: Slow algorithm
        DownloadTask NewTask = Utils.DeepCloneObject(OriginalTask);

        if (NewTask != null) {
            NewTask.Files = new ArrayList<DownloadTaskFile>();

            for (int i = 0; i < OriginalTask.Files.size(); ++i) {
                if (!Indexes.contains(i)) {
                    NewTask.Files.remove(i);
                }
            }
        }

        return NewTask;
    }

    public static AsyncTaskContainer RunTask(DownloadTask Task){
        return new AsyncTaskContainer(new DownloadAsyncTask(Task));
    }
}