package com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.umnicode.samp_launcher.core.SAMP.Components.DefaultTask;
import com.umnicode.samp_launcher.core.SAMP.Components.ExtendedAsyncTask;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

class DownloadAsyncTask extends ExtendedAsyncTask {
    public DownloadTask Task;

    private URLConnection Connection = null;
    private boolean ReadingFromBuffer = false;

    DownloadAsyncTask(DownloadTask Task){
        super();
        this.Task = Task;
    }

    protected Void doInBackground(Void... params) {
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnStarted());

        // Check for internet connection
        if (!this.IsInternetAvailable(this.Task.Param_PingTimeout)){
            Log.println(Log.ERROR, "DownloadSystem", "Internet isn't available");
            this.BroadcastTaskFinished();
            return null;
        }

        // If directory doesn't exist - try to create it
        if (!this.Task.OutDirectory.exists()){
            if (this.Task.OutDirectory.mkdirs()){
                Log.println(Log.INFO, "DownloadSystem", "Output directory doesn't exist, so it was created successfully");
            }else{
                Log.println(Log.ERROR, "DownloadSystem",
                                      "Failed to create output directory - " + this.Task.OutDirectory.toString());

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
                this.Task.Status = new TaskStatus(0, -1.0f, this.Task.FileIndex + 1, this.Task.Files.size());

                // Fire events
                this.BroadcastProgressChanged(); // Force update status

                // Getting file length
                Task.Status.FullSize = Connection.getContentLength();

                // Get filename of file (if server provides it, otherwise set it to default str)
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
                else if (!this.Task.Flag_OverrideIfExist){ // Check for flag
                    this.FinishFileDownload(TaskFileStatus.SUCCESSFUL); // Fire event
                    continue; // Skip this file, it's already downloaded
                }

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

                    Task.Status.Done += Count;
                    Output.write(Data, 0, Count);

                    // Optimization
                    if (Counter == Task.Param_UpdateProgressFrequency) { // Update progress every 64kb
                        Counter = 0;

                        this.BroadcastProgressChanged();
                    } else {
                        Counter++;
                    }
                }

                // When finished, broadcast event not considering optimization counter
                if (Counter != this.Task.Param_UpdateProgressFrequency) this.BroadcastProgressChanged();

                this.ReadingFromBuffer = false;

                // Flushing output
                Output.flush();

                // Closing streams
                Output.close();
                Input.close();

                // Check for cancel
                if (this.isCancelled()) return null;

                // Broadcast finish event
                this.FinishFileDownload(TaskFileStatus.SUCCESSFUL);
            } catch (Exception e) {
                Log.e("DownloadSystem", "Error downloading - " + e.getMessage()); // Send message to log
                if (!this.isCancelled()) this.FinishFileDownload(TaskFileStatus.ERROR);
            }
        } // for (FileIndex)

        // Loop increment value after last cycle, so we fix it here
        this.Task.FileIndex--;

        // After last file downloaded we finish task
        this.BroadcastTaskFinished();

        return null;
    }

    // Overrides
    protected void onCancelled() {
        super.onCancelled();

        this.FinishFileDownload(TaskFileStatus.CANCELED);
        this.Cleanup(true);

        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFinished(true));

        // Container event
        if (this.AfterCancelled != null){
            new Handler(Looper.getMainLooper()).post(() -> this.AfterCancelled.run());
        }
    }

    public void Cancel(Runnable AfterCancelled) {
        this.AfterCancelled = AfterCancelled;

        // Cancel async-task
        this.cancel(true);

        // Close socket connection ( if possible and needed )
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

    public DefaultTask GetTask() {
        return this.Task;
    }

    // Utils
    protected void Cleanup(boolean IsCancelled){
        if (IsCancelled && this.Task.Flag_RemoveAllFilesWhenCancelled) {
            for (DownloadTaskFile file : this.Task.Files) {
                file.OutputResult = TaskFileStatus.CANCELED;
                Utils.RemoveFile(file.OutputFilename);
            }
        }

        if (this.Task.Flag_ResetTaskAfterFinished || (IsCancelled && this.Task.Flag_ResetTaskWhenCancelled)){
            this.Task.Reset(); // Reset task
        }
    }

    private void FinishFileDownload(TaskFileStatus Status){
        if (Status != TaskFileStatus.SUCCESSFUL) {
            // If flag set - remove failed file from storage
            if (this.Task.Flag_RemoveFailedFile) {
                Utils.RemoveFile(this.Task.Files.get(this.Task.FileIndex).OutputFilename);
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

    private boolean IsInternetAvailable(int Timeout) {
        try {
            URL url = new URL("https://google.com");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(Timeout);

            InputStream stream = connection.getInputStream();
            stream.close();

            return true;
        } catch (Exception e) {
            Log.e("DownloadSystem", "Ping failed - " + e.getMessage());
            return false;
        }
    }
};
