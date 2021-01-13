package com.example.samp_launcher.core.SAMP;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ArchiveComponent;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTask;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskCallback;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit;
import com.example.samp_launcher.core.SAMP.Components.AsyncTaskContainer;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadComponent;
import com.example.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskCallback;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskFile;
import com.example.samp_launcher.core.SAMP.Components.TaskStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SAMPInstaller {
    private SAMPInstallerStatus Status = SAMPInstallerStatus.NONE;
    private InstallStatus LastInstallStatus = InstallStatus.NONE;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private File APK_Filepath;

    private final DownloadTask downloadTask;
    private AsyncTaskContainer downloadTaskContainer = null;

    private ExtractTask extractTask = null;
    private AsyncTaskContainer extractTaskContainer = null;

    public SAMPInstaller(Context context){
        this.Callbacks = new ArrayList<>();
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.APK_Filepath = new File("");

        // Setup download component
        Resources resources = context.getResources();
        File defaultDir = new File(Environment.getExternalStorageDirectory().toString() + "/" +
                resources.getString(R.string.app_root_directory_name) + "/" +
                resources.getString(R.string.SAMP_download_directory_name));

        ArrayList<String> URL = new ArrayList<>(Arrays.asList(resources.getString(R.string.SAMP_apk_url),
                                                              resources.getString(R.string.SAMP_data_url)));

        this.downloadTask = DownloadComponent.CreateTask(URL, defaultDir,
                new DownloadTaskCallback() {
                    public void OnStarted() {
                        ChangeStatus(SAMPInstallerStatus.PREPARING);
                    }

                    public void OnFinished(boolean IsCanceled) {
                        // Check does all files downloaded successfully
                        if (!IsCanceled) {
                            for (DownloadTaskFile file : this.Task().Files) {
                                if (file.OutputResult != TaskFileStatus.SUCCESSFUL) {
                                    FinishInstall(InstallStatus.DOWNLOADING_ERROR); // Finish install with error
                                    return;
                                }
                            }

                            // Set APK_Filepath, we will use it on WAITING_FOR_APK_INSTALL stage
                            APK_Filepath = Task().Files.get(0).OutputFilename;

                            // Setup path to data dir
                            File ObbDir = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data");

                            // Try to extract data file
                            ArrayList<ExtractTaskFileInit> TaskFiles = new ArrayList<>();
                            TaskFiles.add(new ExtractTaskFileInit(Task().Files.get(1).OutputFilename,
                                    ObbDir, false));

                            extractTask.SetFilesFromInit(TaskFiles);

                            // Run new container
                            ChangeStatus(SAMPInstallerStatus.EXTRACTING); // Change status
                            extractTaskContainer = ArchiveComponent.RunTask(extractTask);

                            // Remove container
                            downloadTaskContainer = null;
                        }
                    }

                    public void OnChecksFinished() {
                        ChangeStatus(SAMPInstallerStatus.DOWNLOADING);
                    }

                    public void OnFileDownloadStarted() { }
                    public void OnFileDownloadFinished(TaskFileStatus Status) { }

                    public void OnProgressChanged(TaskStatus Status) {
                        // Notify callbacks
                        for (SAMPInstallerCallback Callback : Callbacks) {
                            Handler mainHandler = new Handler(Looper.getMainLooper());

                            Runnable callbackRunnable = () -> Callback.OnDownloadProgressChanged(Status);
                            mainHandler.post(callbackRunnable);
                        }
                    }
                });

        this.extractTask = ArchiveComponent.CreateTask(new ArrayList<>(), new ExtractTaskCallback() {
            public void OnStarted() {
                ChangeStatus(SAMPInstallerStatus.EXTRACTING);
            }
            public void OnFinished(boolean IsCanceled) {
                if (!IsCanceled) {
                    for (ExtractTaskFile file : this.Task().Files) {
                        if (file.OutputResult != TaskFileStatus.SUCCESSFUL) {
                            FinishInstall(InstallStatus.EXTRACTING_ERROR); // Finish install with error
                            return;
                        }
                    }

                    // Remove extract container
                    extractTaskContainer = null;
                }

                //TODO: Waiting for APK install phase
            }

            public void OnFileExtractStarted(ExtractTaskFile File) { }
            public void OnFileExtractFinished(ExtractTaskFile File, TaskFileStatus Status) { }

            public void OnProgressChanged(TaskStatus Status) {
                // Notify callbacks
                for (SAMPInstallerCallback Callback : Callbacks) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    Runnable callbackRunnable = () -> Callback.OnExtractProgressChanged(Status);
                    mainHandler.post(callbackRunnable);
                }
            }
        });
    }

    public void OpenInstalledAPK(){
        // TODO
    }

    // Install management
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
                this.downloadTaskContainer = null;
                CheckCancelState();
            });
        }

        // Stop extract container
        if (this.extractTaskContainer != null){
            this.ChangeStatus(SAMPInstallerStatus.CANCELING_INSTALL);

            this.extractTaskContainer.Cancel(() -> {
                this.extractTaskContainer = null;
                CheckCancelState();
            });
        }

        this.CheckCancelState();
    }

    // Utils
    private void CheckCancelState(){
        if (this.downloadTaskContainer == null && this.extractTaskContainer == null){
            FinishInstall(InstallStatus.CANCELED);
        }
    }

    private void Cleanup(){
        //TODO: Implement this
    }

    private void FinishInstall(InstallStatus Status){
        this.Cleanup();
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.BroadcastInstallFinished(Status);
    }
    private void BroadcastInstallFinished(InstallStatus Status){
        this.LastInstallStatus = Status;

        for (SAMPInstallerCallback Callback : Callbacks) {
            new Handler(Looper.getMainLooper()).post(() -> Callback.OnInstallFinished(this.LastInstallStatus));
        }
    }

    private void ChangeStatus(SAMPInstallerStatus Status){
        this.Status = Status;

        for (SAMPInstallerCallback globalCallback : this.Callbacks){
            new Handler(Looper.getMainLooper()).post(() -> globalCallback.OnStatusChanged(Status));
        }
    }

    // Getters
    public SAMPInstallerStatus GetStatus(){
        return this.Status;
    }
    public TaskStatus GetCurrentTaskStatus(){
        if (this.Status == SAMPInstallerStatus.EXTRACTING) return this.extractTask.Status;
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

    //TODO: Export to Directory function
}