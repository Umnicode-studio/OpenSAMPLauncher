package com.umnicode.samp_launcher.core.SAMP.Components;

public class TaskStatus {
    public float Done = 0;
    public float FullSize = -1.0f;

    public int File = 0;
    public int FilesNumber = 0;

    public TaskStatus(float Done, float FullSize, int File, int FliesNumber){
        this.Done = Done;
        this.FullSize = FullSize;

        this.File = File;
        this.FilesNumber = FliesNumber;
    }

    public static TaskStatus CreateEmpty(int FilesCount){
        if (FilesCount != 0) {
            return new TaskStatus(0, -1.0f, 1, FilesCount);
        }

        return new TaskStatus(0, -1.0f, 0, 0);
    }
}
