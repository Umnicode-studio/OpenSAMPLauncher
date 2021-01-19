package com.umnicode.samp_launcher.core.SAMP;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.umnicode.samp_launcher.BuildConfig;
import com.umnicode.samp_launcher.MainActivity;
import com.umnicode.samp_launcher.R;
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ArchiveComponent;
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTask;
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskCallback;
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile;
import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit;
import com.umnicode.samp_launcher.core.SAMP.Components.AsyncTaskContainer;
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadComponent;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask;
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskCallback;
import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTaskFile;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPPackageStatus;
import com.umnicode.samp_launcher.core.Utils;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SAMPInstaller {
    private SAMPInstallerStatus Status = SAMPInstallerStatus.NONE;
    private InstallStatus LastInstallStatus = InstallStatus.NONE;

    private final Context _Context;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private File APK_Filepath;
    private File Data_Filepath;

    private final DownloadTask downloadTask;
    private AsyncTaskContainer downloadTaskContainer = null;

    private ExtractTask extractTask = null;
    private AsyncTaskContainer extractTaskContainer = null;

    public SAMPInstaller(Context context){
        this.Callbacks = new ArrayList<>();
        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.APK_Filepath = new File("");
        this._Context = context;

        // Setup download component
        Resources resources = context.getResources();
        File defaultDir = GetDefaultDownloadDirectory(resources);

        ArrayList<String> URL = new ArrayList<>(Arrays.asList(resources.getString(R.string.SAMP_apk_url),
                                                              resources.getString(R.string.SAMP_data_url)));

        this.downloadTask = DownloadComponent.CreateTask(URL, defaultDir,
                new DownloadTaskCallback() {
                    public void OnStarted() { }

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
                            File DataDir = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data");

                            // Try to extract data file
                            ArrayList<ExtractTaskFileInit> TaskFiles = new ArrayList<>();
                            TaskFiles.add(new ExtractTaskFileInit(Task().Files.get(1).OutputFilename,
                                    DataDir, false));

                            extractTask.SetFilesFromInit(TaskFiles);

                            // Run new container
                            ChangeStatus(SAMPInstallerStatus.EXTRACTING); // Change status

                            extractTask.Reset(); // Reset task after previous install
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

                    Data_Filepath = this.Task().Files.get(0).Filepath;
                    ChangeStatus(SAMPInstallerStatus.WAITING_FOR_APK_INSTALL);

                    // Remove extract container
                    extractTaskContainer = null;
                }
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
        if (!this.APK_Filepath.exists()){
            this.FinishInstall(InstallStatus.APK_NOT_FOUND);
            return;
        }

        Log.i("SAMPInstaller", "Try to open downloaded APK");
        
        // Get file type
        String MIME = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.GetFileLastExtension(this.APK_Filepath));
        Uri uri = FileProvider.getUriForFile(this._Context,
                BuildConfig.APPLICATION_ID + ".provider", this.APK_Filepath);

        // Install APK
        Intent Install = new Intent(Intent.ACTION_VIEW);

        // Setup flags
        Install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        Install.setDataAndType(uri, MIME);

        this._Context.startActivity(Install);
    }

    // Install management
    public void Install(Context context){
        if (IsInstalled(context.getPackageManager(), context.getResources()) == SAMPPackageStatus.FOUND ||
                this.Status != SAMPInstallerStatus.NONE){
            return;
        }

        // Install with default download task (APK and cache )
        this.InstallImpl(context, this.downloadTask);
    }
    public void InstallOnlyCache(Context context){
        if (IsCacheInstalled() || this.Status != SAMPInstallerStatus.NONE){
            return;
        }

        this.downloadTask.Reset();

        ArrayList<Integer> FilesI = new ArrayList<>();
        FilesI.add(11);

        DownloadTask CacheTask = DownloadComponent.CreateTaskFromFiles(FilesI, this.downloadTask);
        if (CacheTask != null) {
            this.InstallImpl(context, CacheTask);
        }
    }

    public void CancelInstall(){
        this.StopInstall(SAMPInstallerStatus.CANCELING_INSTALL, InstallStatus.CANCELED);
    }

    public void ReCheckInstallResources(Context context){
        if (this.GetStatus() != SAMPInstallerStatus.NONE && this.GetStatus() != SAMPInstallerStatus.CANCELING_INSTALL){
            SAMPPackageStatus Status = IsInstalled(context.getPackageManager(), context.getResources());

            // If SAMP is already installed
            if (Status == SAMPPackageStatus.FOUND && this.Status != SAMPInstallerStatus.NONE){
                // Stop running installation ( with successful status )
                this.StopInstall(SAMPInstallerStatus.CANCELING_INSTALL, InstallStatus.SUCCESSFUL);
                return;
            }

            if (this.GetStatus() == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL ||
                    this.GetStatus() == SAMPInstallerStatus.EXTRACTING) {

                // Check does APK exist anymore ( UndefinedBehavior but... )
                if (!this.APK_Filepath.exists()) {
                    this.FinishInstall(InstallStatus.APK_NOT_FOUND);
                    return;
                }
            }

            Log.i("SAMPInstaller", "ReCheckInstall is successful");
        }
    }

    // Utils
    private void InstallImpl(Context context, DownloadTask Task){ // Without checks
        this.ChangeStatus(SAMPInstallerStatus.PREPARING);

        // Check permissions
        MainActivity Activity = (MainActivity)context;
        Activity.RequestStoragePermission(IsGranted -> {
            if (IsGranted){
                this.APK_Filepath = new File("");

                // Reset task and run it
                this.downloadTask.Reset();
                this.downloadTaskContainer = DownloadComponent.RunTask(downloadTask);
            }else{
                this.FinishInstall(InstallStatus.STORAGE_PERMISSIONS_DENIED); // Error
            }
        });
    }

    // General function for stop
    private void StopInstall(SAMPInstallerStatus WhileStoppingStatus, InstallStatus TargetStatus){
        if (this.Status == SAMPInstallerStatus.NONE) return;

        boolean IsStoppingContainers = false;

        // Stop downloading ( = stop container )
        if (this.downloadTaskContainer != null){
            this.ChangeStatus(WhileStoppingStatus);
            IsStoppingContainers = true;

            this.downloadTaskContainer.Cancel(() -> {
                this.downloadTaskContainer = null;
                CheckStopState(TargetStatus);
            });
        }

        // Stop extract container
        if (this.extractTaskContainer != null){
            this.ChangeStatus(WhileStoppingStatus);
            IsStoppingContainers = true;

            this.extractTaskContainer.Cancel(() -> {
                this.extractTaskContainer = null;
                CheckStopState(TargetStatus);
            });
        }

        if (IsStoppingContainers) this.CheckStopState(TargetStatus);
    }
    private void CheckStopState(InstallStatus TargetStatus){
        if (this.downloadTaskContainer == null && this.extractTaskContainer == null){
            FinishInstall(TargetStatus);
        }
    }

    private void Cleanup(){
        File DownloadDirectory = GetDefaultDownloadDirectory(this._Context.getResources());
        if (DownloadDirectory.exists()){
            if (!DownloadDirectory.delete()){
                Log.e("SAMPInstaller", "Cleanup - failed to remove download directory");
            }
        }
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

    public boolean IsStoragePermissionGranted(Context context){
        return ((MainActivity)context).IsStoragePermissionsGranted();
    }

    // Static tools
    public static File GetDefaultDirectory(Resources resources){
        return new File(Environment.getExternalStorageDirectory().toString() + '/' +
                         resources.getString(R.string.app_root_directory_name));
    }
    public static File GetDefaultDownloadDirectory(Resources resources){
        return new File(GetDefaultDirectory(resources), resources.getString(R.string.SAMP_download_directory_name));
    }

    public static SAMPPackageStatus IsInstalled(PackageManager Manager, Resources resources){
        try {
            Manager.getPackageInfo(resources.getString(R.string.SAMP_package_name), 0);

            if (!IsCacheInstalled()) return SAMPPackageStatus.CACHE_NOT_FOUND;
            return SAMPPackageStatus.FOUND;
        }catch (PackageManager.NameNotFoundException ex){
            return SAMPPackageStatus.NOT_FOUND;
        }
    }
    public static boolean IsCacheInstalled(){
        return new File(Environment.getExternalStorageDirectory().toString() +
                        "/Android/data/com.rockstargames.gtasa/files").exists();
    }

    //TODO: Export to directory function
}