package com.example.samp_launcher.core.SAMP;

public class DownloadStatus{
    public float Downloaded = 0;
    public float FullSize = 0;

    public int File = 0;
    public int FilesNumber = 0;

    DownloadStatus(float Downloaded, float FullSize, int File, int FliesNumber){
        this.Downloaded = Downloaded;
        this.FullSize = FullSize;

        this.File = File;
        this.FilesNumber = FliesNumber;
    }
}