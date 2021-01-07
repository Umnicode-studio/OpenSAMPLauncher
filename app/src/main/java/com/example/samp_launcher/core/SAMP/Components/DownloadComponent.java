package com.example.samp_launcher.core.SAMP.Components;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

interface DownloadCallback{
    void ProgressChanged(DownloadStatus Status);
    void OnFinished(boolean IsSuccessful);
}

class DownloadAsyncTask extends AsyncTask<Void, Void, Void>{
    public final DownloadTask Task;
    public final DownloadComponent.FinishDownload_Class FinishDownload;
    public final DownloadComponentCallback Callback;

    DownloadAsyncTask(DownloadTask Task, DownloadComponent.FinishDownload_Class FinishDownload,
                      DownloadComponentCallback Callback){
        super();

        this.Task = Task;
        this.FinishDownload = FinishDownload;
        this.Callback = Callback;
    }

    protected Void doInBackground(Void... params){
        int Count = 0;
        try {
            URL url = Task.URL.get(Task.FileIndex);
            URLConnection Connection = url.openConnection();
            Connection.connect();

            // Getting file length
            Task.Status.FullSize = Connection.getContentLength();

            // Setup streams
            InputStream Input = new BufferedInputStream(url.openStream(), 8192); // input
            OutputStream Output = new FileOutputStream(new File(Task.OutDirectory, "Test.apk").getAbsoluteFile()); // output TODO

            byte Data[] = new byte[1024];

            int Counter = 0;

            Runnable BroadcastProgressChanged = new Runnable() {
                public void run() {

                }
            };

            while ((Count = Input.read(Data)) != -1) {
                Task.Status.Downloaded += Count;
                Output.write(Data, 0, Count);

                // Optimization
                if (Counter == 30) {
                    Counter = 0;

                    this.BroadcastProgressChanged();
                }else{
                    Counter++;
                }
            }

            // When finished, broadcast event not considering optimization counter
            this.BroadcastProgressChanged();

            // Flushing output
            Output.flush();

            // Closing streams
            Output.close();
            Input.close();

            // Broadcast finish event
            new Handler(Looper.getMainLooper()).post(() -> FinishDownload.Run(true, Task));

        } catch (Exception e) {
            Log.e("Error downloading", "- " + e.getMessage()); // Send message to log
            FinishDownload.Run(false, Task);
        }

        return null;
    }

    private void BroadcastProgressChanged(){
        new Handler(Looper.getMainLooper()).post(new Runnable () {
            public void run () {
                Callback.ProgressChanged(Task.Status);
            }
        });
    }
};

public class DownloadComponent{
    class FinishDownload_Class{
        void Run(boolean IsSuccessful, DownloadTask Task){
            Callback.Finished(IsSuccessful);

            // Select next file
            Task.FileIndex++;

            // Recursion
            DownloadFromQueue();
        }
    }

    private final FinishDownload_Class FinishDownload;
    private final Context _Context;

    private final ArrayList<DownloadTask> Queue;
    private final ArrayList<DownloadTask> History;

    public DownloadComponentCallback Callback;

    public DownloadComponent(Context context){
        // Setup queue
        this.Queue = new ArrayList<>();
        this.History = new ArrayList<>();

        this.FinishDownload = new FinishDownload_Class();

        this._Context = context;
    }

    public void DownloadTo(List<String> URL, File Directory){
        ArrayList<URL> URL_list = new ArrayList<>();
        for (String url_str : URL){
            try {
                URL_list.add(new URL(url_str));
            }catch (MalformedURLException ignore) { };
        }

        // If directory doesn't exist
        if (!Directory.exists()) Directory.mkdir();

        // Create task
        this.Queue.add(new DownloadTask(0, URL_list, Directory, new DownloadStatus(0, -1, 1, URL_list.size())));

        // Force-start task (if not running)
        if (this.Queue.size() == 1){
            this.DownloadFromQueue();
        }
    }

    // Queue worker
    private void DownloadFromQueue(){
        if (this.Queue.isEmpty()) return;

        // Get task
        DownloadTask Task = this.Queue.get(0);
        if (Task.URL.size() <= Task.FileIndex){
            // Remove task
            this.History.add(Task);

            this.Queue.remove(0);
            return;
        }

        // Ok, we can start download next file
        // Update download status
        Task.Status = new DownloadStatus(0, -1.0f, Task.FileIndex + 1, this.Queue.size());
        this.Callback.ProgressChanged(Task.Status);

        DownloadAsyncTask downloadAsyncTask = new DownloadAsyncTask(Task, this.FinishDownload, this.Callback);
        downloadAsyncTask.execute();

        // FinishDownload() will call next item from queue after download of current finish
    }

    // Getters
    public DownloadStatus GetCurrentTaskStatus(){
        return (!this.Queue.isEmpty()) ? this.Queue.get(0).Status : new DownloadStatus(0, -1, 0 ,0);
    }

    public ArrayList <DownloadTask> GetQueue(){
        return this.Queue;
    }
    public ArrayList <DownloadTask> GetHistory(){
        return this.History;
    }
}