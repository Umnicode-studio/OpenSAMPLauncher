package com.example.samp_launcher.core.SAMP;

import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

public interface SAMPInstallerCallback {
    void OnStatusChanged(SAMPInstallerStatus Status);
    void OnDownloadProgressChanged(DownloadStatus Status);
    void OnInstallFinished(InstallStatus Status);
}
