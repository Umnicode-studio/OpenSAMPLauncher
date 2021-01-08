package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

// Non-public wrapper for DownloadTask that own this callback
class DownloadTaskCallbackOwner{
    public DownloadTask Task;
};

public interface DownloadTaskCallback {
    DownloadTaskCallbackOwner Owner = new DownloadTaskCallbackOwner();

    void OnFinished(boolean IsCanceled);

    void OnFileDownloadStarted();
    void OnBufferReadingStarted();
    void OnFileDownloadFinished(boolean Successful);

    void OnProgressChanged(DownloadStatus Status);

    // Get task method
    default DownloadTask Task(){
        return this.Owner.Task;
    }
}
