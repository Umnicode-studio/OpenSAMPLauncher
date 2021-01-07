package com.example.samp_launcher.core.SAMP;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.Components.DownloadComponent;
import com.example.samp_launcher.core.SAMP.Components.DownloadComponentCallback;
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SAMPInstaller {
    private SAMPInstallerStatus Status = SAMPInstallerStatus.NONE;
    private InstallStatus LastInstallStatus = InstallStatus.NONE;
    private final DownloadComponent DownloadComponent;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private String APK_Filepath;

    public SAMPInstaller(Context context){
        this.Callbacks = new ArrayList<>();
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.APK_Filepath = "";

        // Setup download component
        this.DownloadComponent = new DownloadComponent(context);
        this.DownloadComponent.Callback = new DownloadComponentCallback() { //TODO:
            public void Finished(boolean Successful) {
                System.out.println("Finished()");
            }
            public void ProgressChanged(DownloadStatus Status) {
                for (SAMPInstallerCallback Callback : Callbacks){
                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    Runnable callbackRunnable = () -> Callback.OnDownloadProgressChanged(Status);
                    mainHandler.post(callbackRunnable);
                }
            }
        };
    }

    public void OpenInstalledAPK(){
        // TODO
    }

    public void Install(Context context){
        if (IsInstalled(context.getPackageManager(), context.getResources()) && this.Status == SAMPInstallerStatus.NONE){
            return;
        }

        this.ChangeStatus(SAMPInstallerStatus.DOWNLOADING);
        this.DownloadComponent.DownloadTo(Arrays.asList("https://drive.google.com/uc?export=download&id=1wa1SYW81wfirLMQ2APnWcM-XNxnJxYZ3"),
                                          "file://" + Environment.getExternalStorageDirectory() + "/downloads/myfile.apk");

        //TODO: On download finish
        // Change Status to waiting
        //this.ChangeStatus(SAMPInstallerStatus.WAITING_FOR_APK_INSTALL);

        APK_Filepath = "";
    }

    public void CancelInstall(){
        if (this.Status == SAMPInstallerStatus.NONE) return;

        // Update statuses
        this.FinishInstall(InstallStatus.CANCELED);

        // Cleanup

        //TODO:
    }

    // Utils
    private void ChangeStatus(SAMPInstallerStatus Status){
        this.Status = Status;

        for (SAMPInstallerCallback globalCallback : this.Callbacks){
            globalCallback.OnStatusChanged(Status);
        }
    }

    private void FinishInstall(InstallStatus Status){
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.BroadcastInstallFinished(Status);
    }
    private void BroadcastInstallFinished(InstallStatus Status){
        this.LastInstallStatus = Status;

        for (SAMPInstallerCallback Callback : Callbacks) {
            Callback.OnInstallFinished(this.LastInstallStatus);
        }
    }

    // Getters
    public SAMPInstallerStatus GetStatus(){
        return this.Status;
    }
    public DownloadStatus GetDownloadStatus(){
        return this.DownloadComponent.GetDownloadStatus();
    }
    public InstallStatus GetLastInstallStatus() {return this.LastInstallStatus; }

    // Static tools
    public static boolean IsInstalled(PackageManager Manager, Resources resources){
        try {
            Manager.getPackageInfo(resources.getString(R.string.SAMP_package_name), 0);
            return true;
        }catch (PackageManager.NameNotFoundException ex){
            return false;
        }
    }

    //TODO: Export to folder function
}