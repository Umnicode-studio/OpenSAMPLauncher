package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import com.example.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.example.samp_launcher.core.SAMP.Components.TaskStatus;

// Non-public wrapper for DownloadTask that own this callback
class DownloadTaskCallbackOwner{
    public DownloadTask Task;
};

public interface DownloadTaskCallback {
    DownloadTaskCallbackOwner Owner = new DownloadTaskCallbackOwner();

    void OnStarted();
    default void OnChecksFinished() {};

    void OnFinished(boolean IsCanceled);

    void OnFileDownloadStarted();
    default void OnBufferReadingStarted() {};
    void OnFileDownloadFinished(TaskFileStatus Status);

    void OnProgressChanged(TaskStatus Status);

    // Get task method
    default DownloadTask Task(){
        return this.Owner.Task;
    }
}
