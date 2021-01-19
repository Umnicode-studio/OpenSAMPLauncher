package com.umnicode.samp_launcher.core.SAMP;

import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

public interface SAMPInstallerCallback {
    void OnStatusChanged(SAMPInstallerStatus Status);
    void OnDownloadProgressChanged(TaskStatus Status);
    void OnExtractProgressChanged(TaskStatus Status);
    void OnInstallFinished(InstallStatus Status);
}
