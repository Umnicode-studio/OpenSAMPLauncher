package com.example.samp_launcher.core.SAMP;

public class UnzipState {
    float Progress = 0.0f;

    public int File = 0;
    public int FilesNumber = 0;

    UnzipState(float Progress, int File, int FliesNumber){
        this.Progress = Progress;

        this.File = File;
        this.FilesNumber = FliesNumber;
    }
}
