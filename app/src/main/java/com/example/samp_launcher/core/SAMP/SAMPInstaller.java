package com.example.samp_launcher.core.SAMP;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;

import com.example.samp_launcher.R;

import java.util.ArrayList;

interface SAMPInstallerOnDownloadCallback{
    void Finished();
    void ProgressChanged(DownloadState State);
}

public class SAMPInstaller {
    private SAMPInstallerState State;
    private DownloadState DownloadState;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private String APK_Filepath;

    public SAMPInstaller(){
        this.Callbacks = new ArrayList<>();
        this.DownloadState = new DownloadState(0, 0, 0, 0);

        this.ChangeState(SAMPInstallerState.NONE);
        this.APK_Filepath = "";
    }

    private void DownloadTo(String Folder,  Context context, SAMPInstallerOnDownloadCallback Callback){
        this.ChangeState(SAMPInstallerState.DOWNLOADING);

        DownloadState DownloadState = new DownloadState(0, 1000, 1,2);

        // Download to temp folder
        /*DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse("http://www.example.com/myfile.mp3");

        // Create request
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("My File");
        request.setDescription("Downloading");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(false);
        request.setDestinationUri(Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/myfile.mp3"));

        downloadManager.enqueue(request); */

        Callback.ProgressChanged(DownloadState);

        //TODO: On download finish
        // Change state to waiting
        //this.ChangeState(SAMPInstallerState.WAITING_FOR_APK_INSTALL);

        APK_Filepath = "";
    }

    public void OpenInstalledAPK(){

    }

    public boolean Install(Context context){
        if (!IsInstalled(context.getPackageManager(), context.getResources()) && this.State == SAMPInstallerState.NONE){
            this.DownloadTo(context.getResources().getString(R.string.SAMP_download_directory), context,
                            new SAMPInstallerOnDownloadCallback() {
                public void Finished() {

                }
                public void ProgressChanged(DownloadState State) {
                    // Forward event to listeners
                    for (SAMPInstallerCallback Callback : Callbacks){
                        DownloadState = State;
                        Callback.OnDownloadProgressChanged(DownloadState);
                    }
                }
            });
            return true;
        }

        return false;
    }

    public boolean CancelInstall(){
        if (this.State != SAMPInstallerState.NONE) {
            this.ChangeState(SAMPInstallerState.NONE);
            for (SAMPInstallerCallback Callback : Callbacks) {
                Callback.InstallCanceled();
            }

            // Cleanup

            //TODO:
            return true;
        }

        return false;
    }

    // Utils
    private void ChangeState(SAMPInstallerState State){
        this.State = State;

        for (SAMPInstallerCallback globalCallback : this.Callbacks){
            globalCallback.OnStateChanged(State);
        }
    }

    // Getters
    public SAMPInstallerState GetState(){
        return this.State;
    }
    public DownloadState GetDownloadState(){
        return this.DownloadState;
    }

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