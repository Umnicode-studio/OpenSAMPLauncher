package com.example.samp_launcher.core.SAMP;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.example.samp_launcher.R;

import java.util.ArrayList;


interface SAMPInstallerOnDownloadCallback{
    void Finished();
    void ProgressChanged(DownloadStatus Status);
}

class DownloadObserver extends ContentObserver{
    private int DownloadId;
    private SAMPInstallerOnDownloadCallback Callback;
    private DownloadManager _DownloadManager;

    public DownloadObserver(Handler handler, DownloadManager downloadManager, int DownloadId, SAMPInstallerOnDownloadCallback Callback) {
        super(handler);

        this._DownloadManager = downloadManager;
        this.DownloadId = DownloadId;
        this.Callback = Callback;
    }

    public void onChange(boolean selfChange, @Nullable Uri uri) {
        super.onChange(selfChange, uri);

        // Init query
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(this.DownloadId);

        Cursor c = this._DownloadManager.query(query);
        if (c.moveToFirst()) {
            // Get indexes
            int SizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int DownloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

            // Get value from indexes
            float Size = c.getInt(SizeIndex);
            float Downloaded = c.getInt(DownloadedIndex);

            // Get status
            int Status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if (Size != -1){
                this.Callback.ProgressChanged(new DownloadStatus(Downloaded, Size, 0, 0));
            }
        }
    }
}

public class SAMPInstaller {
    private SAMPInstallerStatus Status;
    private DownloadStatus DownloadStatus;

    public ArrayList<SAMPInstallerCallback> Callbacks;
    private String APK_Filepath;

    public SAMPInstaller(){
        this.Callbacks = new ArrayList<>();
        this.DownloadStatus = new DownloadStatus(0, 0, 0, 0);

        this.ChangeStatus(SAMPInstallerStatus.NONE);
        this.APK_Filepath = "";
    }

    private void DownloadTo(String Folder,  Context context, SAMPInstallerOnDownloadCallback Callback){
        this.ChangeStatus(SAMPInstallerStatus.DOWNLOADING);

        DownloadStatus DownloadStatus = new DownloadStatus(0, 1000, 1,2);

        // Download to temp folder
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse("https://drive.google.com/uc?export=download&id=1wa1SYW81wfirLMQ2APnWcM-XNxnJxYZ3");

        // Create request
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("My File");
        request.setDescription("Downloading");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setVisibleInDownloadsUi(false);
        request.setDestinationUri(Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/myfile.apk"));

        downloadManager.enqueue(request);

        Callback.ProgressChanged(DownloadStatus);

        //TODO: On download finish
        // Change Status to waiting
        //this.ChangeStatus(SAMPInstallerStatus.WAITING_FOR_APK_INSTALL);

        APK_Filepath = "";
    }

    public void OpenInstalledAPK(){

    }

    public boolean Install(Context context){
        if (!IsInstalled(context.getPackageManager(), context.getResources()) && this.Status == SAMPInstallerStatus.NONE){
            this.DownloadTo(context.getResources().getString(R.string.SAMP_download_directory), context,
                            new SAMPInstallerOnDownloadCallback() {
                public void Finished() {

                }
                public void ProgressChanged(DownloadStatus Status) {
                    // Forward event to listeners
                    for (SAMPInstallerCallback Callback : Callbacks){
                        DownloadStatus = Status;
                        Callback.OnDownloadProgressChanged(DownloadStatus);
                    }
                }
            });
            return true;
        }

        return false;
    }

    public boolean CancelInstall(){
        if (this.Status != SAMPInstallerStatus.NONE) {
            this.ChangeStatus(SAMPInstallerStatus.NONE);
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
    private void ChangeStatus(SAMPInstallerStatus Status){
        this.Status = Status;

        for (SAMPInstallerCallback globalCallback : this.Callbacks){
            globalCallback.OnStatusChanged(Status);
        }
    }

    // Getters
    public SAMPInstallerStatus GetStatus(){
        return this.Status;
    }
    public DownloadStatus GetDownloadStatus(){
        return this.DownloadStatus;
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