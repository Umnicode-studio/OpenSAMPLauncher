package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import com.example.samp_launcher.core.SAMP.Components.TaskFileStatus;

import java.io.File;
import java.net.URL;

public class DownloadTaskFile {
    public File OutputFilename = null;
    public TaskFileStatus OutputResult = TaskFileStatus.NONE;

    public URL url;

    DownloadTaskFile(URL url){
        this.url = url;
    }
}
