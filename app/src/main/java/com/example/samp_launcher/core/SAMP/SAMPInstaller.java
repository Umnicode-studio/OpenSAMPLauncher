package com.example.samp_launcher.core.SAMP;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadAsyncTaskContainer;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadComponent;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadFileStatus;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskCallback;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskFile;
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class SAMPInstaller {
    private SAMPInstallerStatus Status = SAMPInstallerStatus.NONE;
    private InstallStatus LastInstallStatus = InstallStatus.NONE;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private File APK_Filepath;

    private final DownloadTask downloadTask;
    private DownloadAsyncTaskContainer downloadTaskContainer = null;

    public SAMPInstaller(Context context){
        this.Callbacks = new ArrayList<>();
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.APK_Filepath = new File("");

        // Setup download component
        Resources resources = context.getResources();
        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" +
                resources.getString(R.string.app_root_directory_name) + "/" +
                resources.getString(R.string.SAMP_download_directory_name));

        this.downloadTask = DownloadComponent.CreateTask(Collections.singletonList(resources.getString(R.string.SAMP_apk_url)), file,
                new DownloadTaskCallback() {
                    public void OnStarted() {
                        ChangeStatus(SAMPInstallerStatus.PREPARING);
                    }
                    public void OnFinished(boolean IsCanceled) {
                        // Check does all files downloaded successfully
                        if (!IsCanceled) {
                            for (DownloadTaskFile file : this.Task().Files) {
                                if (file.OutputResult != DownloadFileStatus.SUCCESSFUL) {
                                    FinishInstall(InstallStatus.DOWNLOADING_ERROR); // Finish install with error
                                    return;
                                }
                            }

                            // Remove container
                            downloadTaskContainer = null;

                            // Set APK_Filepath, we will use it on WAITING_FOR_APK_INSTALL stage
                            APK_Filepath = Task().Files.get(0).OutputFilename;

                            // Unzip obb file
                            //TODO:
                        }
                    }
                    public void OnChecksFinished() {
                        ChangeStatus(SAMPInstallerStatus.DOWNLOADING);
                    }

                    public void OnFileDownloadStarted() {
                    }
                    public void OnFileDownloadFinished(DownloadFileStatus Status) {
                        // Do nothing
                    }

                    public void OnProgressChanged(DownloadStatus Status) {
                        // Notify callbacks
                        for (SAMPInstallerCallback Callback : Callbacks){
                            Handler mainHandler = new Handler(Looper.getMainLooper());

                            Runnable callbackRunnable = () -> Callback.OnDownloadProgressChanged(Status);
                            mainHandler.post(callbackRunnable);
                        }
                    }
                });
    }

    public void OpenInstalledAPK(){
        // TODO
    }

    public void Install(Context context){
        if (IsInstalled(context.getPackageManager(), context.getResources()) || this.Status != SAMPInstallerStatus.NONE){
            return;
        }

        this.downloadTaskContainer = DownloadComponent.RunTask(this.downloadTask);
        APK_Filepath = new File("");
    }

    public void CancelInstall(){
        if (this.Status == SAMPInstallerStatus.NONE) return;

        // Stop downloading ( = stop container )
        if (this.downloadTaskContainer != null){
            this.ChangeStatus(SAMPInstallerStatus.CANCELING_INSTALL);

            this.downloadTaskContainer.Cancel(() -> {
                FinishInstall(InstallStatus.CANCELED);
                this.downloadTaskContainer = null;
            });
        }else{
            FinishInstall(InstallStatus.CANCELED);
        }

        //TODO:
    }

    // Utils
    private void ChangeStatus(SAMPInstallerStatus Status){
        this.Status = Status;

        for (SAMPInstallerCallback globalCallback : this.Callbacks){
            new Handler(Looper.getMainLooper()).post(() -> globalCallback.OnStatusChanged(Status));
        }
    }

    private void FinishInstall(InstallStatus Status){
        this.ChangeStatus(SAMPInstallerStatus.NONE);

        // Clean-up
        //TODO: Clean-up

        this.BroadcastInstallFinished(Status);
    }
    private void BroadcastInstallFinished(InstallStatus Status){
        this.LastInstallStatus = Status;

        for (SAMPInstallerCallback Callback : Callbacks) {
            new Handler(Looper.getMainLooper()).post(() -> Callback.OnInstallFinished(this.LastInstallStatus));
        }
    }

    // Getters
    public SAMPInstallerStatus GetStatus(){
        return this.Status;
    }
    public DownloadStatus GetDownloadStatus(){
        return this.downloadTask.Status;
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