package com.example.samp_launcher.core.SAMP;

import com.example.samp_launcher.core.SAMP.Components.TaskStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

public interface SAMPInstallerCallback {
    void OnStatusChanged(SAMPInstallerStatus Status);
    void OnDownloadProgressChanged(TaskStatus Status);
    void OnExtractProgressChanged(TaskStatus Status);
    void OnInstallFinished(InstallStatus Status);
}
