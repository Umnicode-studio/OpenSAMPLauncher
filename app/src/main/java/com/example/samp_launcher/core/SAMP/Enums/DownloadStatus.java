package com.example.samp_launcher.core.SAMP.Enums;

public class DownloadStatus{
    public float Downloaded = 0;
    public float FullSize = -1.0f;

    public int File = 0;
    public int FilesNumber = 0;

    public DownloadStatus(float Downloaded, float FullSize, int File, int FliesNumber){
        this.Downloaded = Downloaded;
        this.FullSize = FullSize;

        this.File = File;
        this.FilesNumber = FliesNumber;
    }
}