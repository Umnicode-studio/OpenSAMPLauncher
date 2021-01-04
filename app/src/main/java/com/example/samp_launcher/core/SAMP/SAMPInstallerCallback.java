package com.example.samp_launcher.core.SAMP;

public interface SAMPInstallerCallback {
    void OnStateChanged(SAMPInstallerState State);

    void OnDownloadProgressChanged(DownloadState State);
    void OnUnzipProgressChanged(UnzipState State);

    void InstallFinished();
    void InstallCanceled();
}
