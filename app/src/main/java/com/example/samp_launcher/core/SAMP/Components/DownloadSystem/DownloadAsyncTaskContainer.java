package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

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
import java.net.URLConnection;

class DownloadAsyncTask extends AsyncTask<Void, Void, Void> {
    public final DownloadTask Task;
    public Runnable OnCancelFinish_Container = null;

    DownloadAsyncTask(DownloadTask Task){
        super();

        this.Task = Task;
    }

    protected Void doInBackground(Void... params) {
        for (; Task.FileIndex <= Task.Files.size(); ++Task.FileIndex) {
            if (Task.FileIndex == Task.Files.size()){
                // Finish task
                Task.Callback.OnFinished(false);
                break;
            }

            int Count = 0;
            try {
                DownloadTaskFile file = Task.Files.get(Task.FileIndex);
                URLConnection Connection = file.url.openConnection();
                Connection.connect();

                // Init status
                this.Task.Status = new DownloadStatus(0, -1.0f, this.Task.FileIndex + 1, this.Task.Files.size());

                // Fire events
                new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileDownloadStarted());
                new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnProgressChanged(Task.Status)); // Force update status

                // Check for cancel
                if (isCancelled()) return null;

                // Getting file length
                Task.Status.FullSize = Connection.getContentLength();

                // Get filename
                String Filename;

                String contentDispositionRaw = Connection.getHeaderField("Content-Disposition");
                if (contentDispositionRaw != null && contentDispositionRaw.contains("filename=")) {
                    Filename = contentDispositionRaw.split("filename=")[1];
                    // Remove all next params in header
                    if (Filename.contains(";")) {
                        Filename = Filename.split(";")[0];
                    }

                    // If filename contains spaces ( => quotes ) replace it with _ and remove quotes
                    Filename = Filename.replace(' ', '_');
                    Filename = Filename.replace("\"", "");
                } else {
                    Filename = "_download_" + (Task.FileIndex + 1) + "_" + Task.Files.size(); // Gen default str. Example: ( _download_1_2 )
                }

                file.OutputFilename = new File(Task.OutDirectory, Filename).getAbsoluteFile();

                // Setup streams
                InputStream Input = new BufferedInputStream(file.url.openStream(), 8192); // input
                OutputStream Output = new FileOutputStream(file.OutputFilename); // output

                // Check for cancel ( opening streams can be very long operation )
                if (isCancelled()) return null;

                new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnBufferReadingStarted()); // broadcast event

                byte Data[] = new byte[1024];

                int Counter = 0;
                while ((Count = Input.read(Data)) != -1) {
                    // Check for cancel
                    if (this.isCancelled()) return null;

                    Task.Status.Downloaded += Count;
                    Output.write(Data, 0, Count);

                    // Optimization
                    if (Counter == 30) {
                        Counter = 0;

                        this.BroadcastProgressChanged();
                    } else {
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

                // Check for cancel
                if (this.isCancelled()) return null;

                // Broadcast finish event
                this.FinishDownload(true);
            } catch (Exception e) {
                Log.e("Error downloading", "- " + e.getMessage()); // Send message to log
                this.FinishDownload(false);
            }
        }

        return null;
    }

    protected void onCancelled() {
        super.onCancelled();

        System.out.println("WorkerCanceled()"); //TODO:

        this.Cleanup();
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFinished(true));

        // Container event
        if (this.OnCancelFinish_Container != null) this.OnCancelFinish_Container.run();
    }

    // Utils
    public void Cleanup(){
        File file = this.Task.Files.get(this.Task.FileIndex).OutputFilename;
        if (file != null) file.delete();
    }

    private void FinishDownload(boolean IsSuccessful){
        // Set current file status
        this.Task.Files.get(Task.FileIndex).OutputResult = IsSuccessful;
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileDownloadFinished(IsSuccessful));
    }

    private void BroadcastProgressChanged(){
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnProgressChanged(Task.Status));
    }
};

public class DownloadAsyncTaskContainer {
    private final DownloadAsyncTask AsyncTask;

    DownloadAsyncTaskContainer(DownloadAsyncTask AsyncTask){
        this.AsyncTask = AsyncTask;
        this.AsyncTask.execute(); // Run task

        System.out.println("container running()"); //TODO
    }

    public void Cancel(Runnable OnFinish){
        this.AsyncTask.OnCancelFinish_Container = OnFinish;
        this.AsyncTask.cancel(true); // Cancel task
    }
    public DownloadTask GetTask(){
        return this.AsyncTask.Task;
    }
}
