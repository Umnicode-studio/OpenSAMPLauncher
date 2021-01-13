package com.example.samp_launcher.core.SAMP.Components;

public abstract class DefaultTask{
    public int FileIndex;
    public TaskStatus Status;

    public boolean Flag_RemoveAllFilesWhenCancelled = false;
    public boolean Flag_RemoveFailedFile = true;
    public boolean Flag_ResetTaskWhenCanceled = true;
    public boolean Flag_OverrideIfExist = false;

    public int Param_UpdateProgressFrequency = 64; // Every 64 KB

    public abstract void Reset();
}