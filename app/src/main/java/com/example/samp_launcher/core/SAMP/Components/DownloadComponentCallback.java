package com.example.samp_launcher.core.SAMP.Components;
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

public interface DownloadComponentCallback{
    void Finished(boolean Successful);
    void ProgressChanged(DownloadStatus Status);
}