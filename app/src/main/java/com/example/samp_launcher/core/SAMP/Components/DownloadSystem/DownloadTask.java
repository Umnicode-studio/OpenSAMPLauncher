package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import com.example.samp_launcher.core.SAMP.Components.DefaultTask;
import com.example.samp_launcher.core.SAMP.Components.TaskStatus;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class DownloadTask extends DefaultTask {
    public File OutDirectory;

    public ArrayList<DownloadTaskFile> Files;
    public DownloadTaskCallback Callback;

    public int Param_PingTimeout = 500;

    DownloadTask(int FileIndex, ArrayList<URL> URL_List, File OutDir, TaskStatus Status, DownloadTaskCallback Callback){
        this.FileIndex = FileIndex;

        this.Files = new ArrayList<>();
        for (URL url : URL_List) {
            this.Files.add(new DownloadTaskFile(url));
        }

        this.OutDirectory = OutDir;
        this.Status = Status;

        // Set callback owner
        this.SetCallback(Callback);
    }

    public void SetCallback(DownloadTaskCallback Callback){
        if (Callback != this.Callback) {
            this.Callback.Owner.Task = null; // Remove owner from old callback
            this.Callback = Callback;
            this.Callback.Owner.Task = this; // Set this as a owner in new callback
        }
    }

    public void Reset(){
        // Reset
        this.FileIndex = 0;

        for (int c = 0; c < this.Files.size(); ++c){
            this.Files.set(c, new DownloadTaskFile(this.Files.get(c).url));
        }
    }
}