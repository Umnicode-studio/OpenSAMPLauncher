package com.example.samp_launcher.core.SAMP.Components;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

import java.net.URI;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

interface DownloadProgressCheckerCallback{
    void ProgressChanged(float Downloaded, float FullSize);
}
class DownloadProgressChecker {
    private final long DownloadId;
    private final DownloadProgressCheckerCallback Callback;
    private final DownloadManager _DownloadManager;

    private Timer _Timer;
    private TimerTask CheckerTask;
    
    private int Delay = 0;
    private boolean IsWatching = false;

    public DownloadProgressChecker(DownloadManager downloadManager, long DownloadId, int Delay, Context context,
                                   DownloadProgressCheckerCallback Callback) {
        this._DownloadManager = downloadManager;
        this.DownloadId = DownloadId;
        this.Callback = Callback;
        this.Delay = Delay;

        // Create timer
        this._Timer = new Timer();

        // Create checker
        this.CheckerTask = new TimerTask() {
            public void run() {
                CheckFunction();
            }
        };
    }

    // Destructor
    protected void finalize() throws Throwable {
        super.finalize();

        // Unregister
        this.SetWatchingState(false);
    }

    public void SetWatchingState(boolean IsEnabled){
        if (this.IsWatching != IsEnabled){
            if (IsEnabled) this._Timer.schedule(this.CheckerTask, 0, this.Delay);
            else this._Timer.cancel();

            this.IsWatching = IsEnabled;
        }
    }
    
    // Getters
    public boolean IsWatching(){
        return this.IsWatching;
    }
    public int GetDelay(){
        return this.Delay;
    }

    private void CheckFunction(){
        System.out.println("checkFunction() - " + this.DownloadId);

        // Init query
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(this.DownloadId);

        Cursor c = this._DownloadManager.query(query);
        if (c.moveToFirst()) {
            // Get indexes
            int SizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int DownloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

            System.out.println(c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE)));

            // Get value from indexes
            float Size = c.getLong(SizeIndex);
            float Downloaded = c.getLong(DownloadedIndex);

            System.out.println("Size - " + Size);
            this.Callback.ProgressChanged(Downloaded, Size);
        }

        // Closing cursor
        c.close();
    }
}

class DownloadTask{
    public String OutDirectory = "";

    public int FileIndex = 0;
    public List<Uri> URI;

    DownloadTask(int FileIndex, ArrayList<Uri> URI_List, String OutDir){
        this.FileIndex = FileIndex;
        this.URI = URI_List;
        this.OutDirectory = OutDir;
    }
}

public class DownloadComponent{
    interface CompleteReceiverCallback{
        void OnFinish(boolean IsSuccessful, Uri localUri);
    }
    class CompleteReceiver extends BroadcastReceiver {
        public CompleteReceiverCallback Callback = null;
        
        public void onReceive(Context context, Intent intent) {
            long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (this.Callback != null && completedId == DownloadId) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(DownloadId);

                Cursor c = _DownloadManager.query(query);

                if (c.moveToFirst()) {
                    int Status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    Uri uri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));

                    // Broadcast finish event
                    switch (Status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            this.Callback.OnFinish(true, uri);
                        case DownloadManager.STATUS_FAILED:
                            this.Callback.OnFinish(false, uri);
                            // Other statuses like PAUSED don't matter here, but you can implement them
                    }
                }
            }
        }
    };

    private final DownloadManager _DownloadManager;
    private final Context _Context;

    private long DownloadId = -1;
    private DownloadStatus DownloadStatus;

    private final CompleteReceiver _CompleteReceiver;
    private DownloadProgressChecker Checker = null;

    private final ArrayList<DownloadTask> Queue;

    public DownloadComponentCallback Callback;

    public DownloadComponent(Context context){
        // Setup queue
        this.Queue = new ArrayList<>();

        this._Context = context;
        this._DownloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

        this.DownloadStatus = new DownloadStatus(0, 0, 0, 0);

        // Register receiver
        this._CompleteReceiver = new CompleteReceiver();
        this._Context.registerReceiver(this._CompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    // Destructor
    protected void finalize() throws Throwable {
        super.finalize();

        // Unregister
        this._Context.unregisterReceiver(this._CompleteReceiver);
    }

    public boolean DownloadTo(List<String> URI, String Filepath){
        System.out.println("Start downloading... Amount of files is " + URI.size()); //TODO:
        
        ArrayList<Uri> URI_list = new ArrayList<>();
        for (String uri_str : URI){
            URI_list.add(Uri.parse(uri_str));
        }
        
        // Create task
        this.Queue.add(new DownloadTask(0, URI_list, Filepath));

        // Force-start task (if not running)
        if (this.Queue.size() == 1){
            this.DownloadFromQueue();
        }

        return true;
    }

    // Queue worker
    private void DownloadFromQueue(){
        if (this.Queue.isEmpty()) return;

        // Get task
        DownloadTask Task = this.Queue.get(0);
        if (Task.URI.size() <= Task.FileIndex){
            // Remove task
            this.Queue.remove(0);
            return;
        }

        // Update download status
        this.DownloadStatus = new DownloadStatus(0, 0, Task.FileIndex + 1, this.Queue.size());
        
        // Create request
        DownloadManager.Request request = new DownloadManager.Request(Task.URI.get(Task.FileIndex));
        request.setTitle("File #" + (Task.FileIndex + 1));
        request.setDescription("OpenSAMP launcher downloading");

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(false);

        request.setDescription(Task.OutDirectory);

        this.DownloadId = this._DownloadManager.enqueue(request);
        System.out.println("Id of download - " + this.DownloadId); //TODO:

        // Register receiver callback
        this._CompleteReceiver.Callback = new CompleteReceiverCallback() {
            public void OnFinish(boolean IsSuccessful, Uri localUri) {
                Callback.Finished(IsSuccessful);
                DownloadId = -1;

                // Stop watching
                if (Checker != null){
                    Checker.SetWatchingState(false);
                    Checker = null;
                }

                // Move file to filepath
                if (!Task.OutDirectory.isEmpty()){
                    //TODO: Move to external storage
                }

                // Select next file
                Task.FileIndex++;

                // Recursion
                DownloadFromQueue();
            }
        };
        
        // Create progress watcher
        this.Checker = new DownloadProgressChecker(this._DownloadManager, this.DownloadId, 1000, this._Context,
                new DownloadProgressCheckerCallback() {
                    public void ProgressChanged(float Downloaded, float FullSize) {
                        Callback.ProgressChanged(new DownloadStatus(Downloaded, FullSize, Task.FileIndex + 1, Task.URI.size())); 
                    }
                });
        this.Checker.SetWatchingState(true);

        // FinishDownload() will call next item from queue after download of current finish
    }

    // Getters
    public DownloadStatus GetDownloadStatus(){
        return this.DownloadStatus;
    }
}