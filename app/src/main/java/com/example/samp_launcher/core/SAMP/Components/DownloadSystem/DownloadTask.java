package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class DownloadTask{
    public File OutDirectory;

    public int FileIndex;
    public ArrayList<DownloadTaskFile> Files;

    public DownloadStatus Status;
    public DownloadTaskCallback Callback;

    public boolean Flag_RemoveAllFilesWhenCancelled = false;
    public boolean Flag_RemoveFailedToDownloadFile = true;

    DownloadTask(int FileIndex, ArrayList<URL> URL_List, File OutDir, DownloadStatus Status, DownloadTaskCallback Callback){
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
}