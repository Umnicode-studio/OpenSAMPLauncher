package com.example.samp_launcher.core.SAMP.Components;

import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class DownloadTask{
    public File OutDirectory;

    public int FileIndex;
    public ArrayList<java.net.URL> URL;

    public DownloadStatus Status;

    DownloadTask(int FileIndex, ArrayList<URL> URL_List, File OutDir, DownloadStatus Status){
        this.FileIndex = FileIndex;
        this.URL = URL_List;
        this.OutDirectory = OutDir;

        this.Status = Status;
    }
}