package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

class DownloadAsyncTask extends AsyncTask<Void, Void, Void> {
    public final DownloadTask Task;
    public Runnable OnCancelFinish_Container = null;

    private URLConnection Connection = null;
    private boolean ReadingFromBuffer = false;

    DownloadAsyncTask(DownloadTask Task){
        super();
        this.Task = Task;
    }

    protected Void doInBackground(Void... params) {
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnStarted());

        // Check for internet connection
        if (!this.IsInternetAvailable()){
            Log.println(Log.ERROR, "DownloadSystem", "Internet isn't available");
            this.BroadcastTaskFinished();
            return null;
        }

        // If directory doesn't exist - try to create it
        if (!this.Task.OutDirectory.exists()){
            if (this.Task.OutDirectory.mkdirs()){
                Log.println(Log.INFO, "DownloadSystem", "Output directory doesn't exist, so it was created successfully");
            }else{
                Log.println(Log.ERROR, "DownloadSystem", "Failed to create output directory");

                this.BroadcastTaskFinished();
                return null;
            }
        }

        // Fire event after all init checks are finished
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnChecksFinished());

        for (; Task.FileIndex < Task.Files.size(); ++Task.FileIndex) {
            int Count = 0;
            try {
                // Fire event
                new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileDownloadStarted());

                DownloadTaskFile file = Task.Files.get(Task.FileIndex);
                Connection = file.url.openConnection();
                Connection.connect();

                // Check for cancel
                if (isCancelled()) return null;

                // Init status
                this.Task.Status = new DownloadStatus(0, -1.0f, this.Task.FileIndex + 1, this.Task.Files.size());

                // Fire events
                this.BroadcastProgressChanged(); // Force update status

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

                // We don't check the result of file creation because we will do it later ( when open output-stream)
                if (!file.OutputFilename.exists()) file.OutputFilename.createNewFile();

                // Setup streams
                InputStream Input = new BufferedInputStream(file.url.openStream(), 8192); // input
                OutputStream Output = new FileOutputStream(file.OutputFilename); // output

                // Check for cancel ( opening streams can be very long operation )
                if (isCancelled()) return null;

                new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnBufferReadingStarted()); // broadcast event
                this.ReadingFromBuffer = true;

                byte Data[] = new byte[1024];

                int Counter = 0;
                while ((Count = Input.read(Data)) != -1) {
                    // Check for cancel
                    if (this.isCancelled()) return null;

                    Task.Status.Downloaded += Count;
                    Output.write(Data, 0, Count);

                    // Optimization
                    if (Counter == 64) { // Update progress every 64kb
                        Counter = 0;

                        this.BroadcastProgressChanged();
                    } else {
                        Counter++;
                    }
                }

                // When finished, broadcast event not considering optimization counter
                this.BroadcastProgressChanged();

                this.ReadingFromBuffer = false;

                // Flushing output
                Output.flush();

                // Closing streams
                Connection.getInputStream().close();
                Output.close();
                Input.close();

                // Check for cancel
                if (this.isCancelled()) return null;

                // Broadcast finish event
                this.FinishFileDownload(DownloadFileStatus.SUCCESSFUL);
            } catch (Exception e) { //TODO: Exception parse
                Log.e("DownloadSystem", "Error downloading - " + e.getMessage()); // Send message to log
                if (!this.isCancelled()) this.FinishFileDownload(DownloadFileStatus.ERROR);
            }

            // On last file we finish task
            if (Task.FileIndex == Task.Files.size() - 1){
                this.BroadcastTaskFinished(); // Finish task
                break;
            }
        }

        return null;
    }

    protected void onCancelled() {
        super.onCancelled();

        this.FinishFileDownload(DownloadFileStatus.CANCELED);
        this.Cleanup();

        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFinished(true));

        // Container event
        if (this.OnCancelFinish_Container != null) this.OnCancelFinish_Container.run();
    }

    public void Cancel() {
        this.cancel(true);
        if (this.Connection != null && !this.ReadingFromBuffer){
            if (this.Connection instanceof HttpURLConnection){
                try {
                    new Thread(() -> ((HttpURLConnection) Connection).disconnect()).start(); // Does it safe?
                }catch (Exception e){
                    Log.println(Log.ERROR, "DownloadSystem", "Error when cancelling - " + e.toString());
                }
            }
        }
    }

    // Utils
    private void Cleanup(){
        if (this.Task.Flag_RemoveAllFilesWhenCancelled) {
            for (DownloadTaskFile file : this.Task.Files) {
                this.RmFile(file.OutputFilename);
            }
        }
    }

    private void RmFile(File file){
        if (file != null) file.delete();
    }

    private void FinishFileDownload(DownloadFileStatus Status){
        if (Status != DownloadFileStatus.SUCCESSFUL) {
            // If flag set - remove failed file from storage
            if (this.Task.Flag_RemoveFailedToDownloadFile) {
                this.RmFile(this.Task.Files.get(this.Task.FileIndex).OutputFilename);
            }
        }

        // Set current file status
        this.Task.Files.get(this.Task.FileIndex).OutputResult = Status;
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileDownloadFinished(Status));
    }

    private void BroadcastProgressChanged(){
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnProgressChanged(Task.Status));
    }
    private void BroadcastTaskFinished(){
        new Handler(Looper.getMainLooper()).post(() -> Task.Callback.OnFinished(false));
    }

    private boolean IsInternetAvailable() {
        try {
            URL url = new URL("https://google.com");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(500); //TODO: Move as settings to task

            connection.connect();
            connection.getInputStream().close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }
};

public class DownloadAsyncTaskContainer {
    private final DownloadAsyncTask AsyncTask;

    DownloadAsyncTaskContainer(DownloadAsyncTask AsyncTask){
        this.AsyncTask = AsyncTask;
        this.AsyncTask.execute(); // Run task
    }

    public void Cancel(Runnable OnFinish){
        this.AsyncTask.OnCancelFinish_Container = OnFinish;
        this.AsyncTask.Cancel(); // Cancel task
    }
    public DownloadTask GetTask(){
        return this.AsyncTask.Task;
    }
}
