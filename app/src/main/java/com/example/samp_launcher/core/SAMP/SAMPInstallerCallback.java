package com.example.samp_launcher.core.SAMP;

public interface SAMPInstallerCallback {
    void OnStatusChanged(SAMPInstallerStatus Status);

    void OnDownloadProgressChanged(DownloadStatus Status);

    void InstallFinished();
    void InstallCanceled();
}
