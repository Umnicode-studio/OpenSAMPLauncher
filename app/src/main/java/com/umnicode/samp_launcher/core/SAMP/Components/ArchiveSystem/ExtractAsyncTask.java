package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile;
import com.umnicode.samp_launcher.core.SAMP.Components.DefaultTask;
import com.umnicode.samp_launcher.core.SAMP.Components.ExtendedAsyncTask;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

class ExtractAsyncTask extends ExtendedAsyncTask {
    public final ExtractTask Task;

    ExtractAsyncTask(ExtractTask Task){
        this.Task = Task;
    }

    protected Void doInBackground(Void... voids) {
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnStarted());

        for (; Task.FileIndex < Task.Files.size(); ++Task.FileIndex) {
            ExtractTaskFile file = Task.Files.get(Task.FileIndex);

            // Check for cancel
            if (this.isCancelled()) return null;

            // File extract start event
            new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileExtractStarted(file));

            // Init status
            this.Task.Status = new TaskStatus(0, -1.0f,  this.Task.FileIndex + 1, this.Task.Files.size());

            // Checks
            if (!file.Filepath.exists() || !file.Filepath.isFile()) {
                Log.e("ExtractSystem", "Incorrect filepath - " + file.Filepath.toString());
                this.FinishFileExtract(TaskFileStatus.ERROR);
                continue;
            }

            if (file.OutputDirectory.exists() && !file.OutputDirectory.isDirectory()) {
                Log.e("ExtractSystem", "OutputDirectory is not a directory - " + file.OutputDirectory.toString());
                this.FinishFileExtract(TaskFileStatus.ERROR);
                continue;
            }

            // Broadcast event
            new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnChecksFinished());

            // Setup out directory
            File OutDir;

            // Create containing folder if needed
            if (file.CreateContainingDirectory){
                OutDir = new File(file.OutputDirectory, Utils.GetFileNameWithoutExtension(file.Filepath, false));
            }
            else OutDir = file.OutputDirectory;

            // Create dirs ( if they don't exist )
            if (!OutDir.exists()) {
                if (!OutDir.mkdirs()) {
                    Log.e("ExtractSystem", "Failed to create dirs - " + new File(OutDir, "dummy.value").toString());
                    this.FinishFileExtract(TaskFileStatus.ERROR);
                    continue;
                } else {
                    Log.i("ExtractSystem", "Created directories for path - " + OutDir.toString()); // Log
                }
            }

            // Check for cancel
            if (this.isCancelled()) return null;

            // Determine archive type
            ArchiveType Type = ArchiveComponent.GetTypeOfArchive(file.Filepath);
            TaskFileStatus Result = TaskFileStatus.SUCCESSFUL;

            // Extract archive depend on its type
            if (Type == ArchiveType.ZIP){
                try {
                    // Extract zip
                    ZipFile Zip = new ZipFile(file.Filepath);

                    ArrayList<? extends ZipEntry> Entries = Collections.list(Zip.entries());

                    // Init done/full size
                    Task.Status.Done = 0;
                    for (ZipEntry zipEntry : Entries){ // Iterate over zip file to get size of entries
                        if (!zipEntry.isDirectory()) Task.Status.FullSize += zipEntry.getSize();
                    }

                    // Notify that we initiated status ( done/full size )
                    this.BroadcastProgressChanged();

                    for (ZipEntry Entry : Entries) {
                        // Check for cancel
                        if (this.isCancelled()) return null;
                        File Path = new File(OutDir, Entry.getName());

                        if (Entry.isDirectory()) {
                            // Create new dirs ( if path don't exist )
                            if (!Path.exists() && !Path.mkdirs()) {
                                Result = TaskFileStatus.ERROR;
                                throw new Exception("Can't create directory - " + Path.toString());
                            }
                        } else{
                            // Skip current entry if flag set
                            if (Path.exists() && Path.length() == Entry.getSize() && !this.Task.Flag_OverrideIfExist){
                                // Add entry size to done ( because we skip entry )
                                Task.Status.Done += Entry.getSize();
                                this.BroadcastProgressChanged();

                                continue;
                            }

                            try {
                                // Open streams
                                InputStream Input = Zip.getInputStream(Entry); // Already unzipped file

                                // Copy content from zip to output file
                                OutputStream Output = new FileOutputStream(Path);

                                byte Buffer[] = new byte[1024];
                                int Len, Counter = 0;

                                while ((Len = Input.read(Buffer)) > 0) {
                                    if (this.isCancelled()) return null;

                                    // Write buffer to out stream
                                    Output.write(Buffer, 0, Len);
                                    Task.Status.Done += Len;

                                    // Basic optimization trick
                                    if (Counter == this.Task.Param_UpdateProgressFrequency) { // As default this value set to 64 KB
                                        this.BroadcastProgressChanged();
                                        Counter = 0;
                                    }else{
                                        Counter++;
                                    }
                                }

                                // Close output stream
                                Output.flush();
                                Output.close();

                                // Close input streams
                                Input.close();

                                // Update status after timer
                                if (Counter != this.Task.Param_UpdateProgressFrequency) this.BroadcastProgressChanged();
                            } catch (IOException ex){
                                Log.e("ZipToolkit", "Can't write file - " + Path.toString() + "; Reason - " + ex.getMessage());
                                Result = TaskFileStatus.ERROR;
                            }
                        }
                    }
                }catch (Exception e){
                    Log.e("ZipToolkit", Objects.requireNonNull(e.getMessage()));
                }
            }

            // File extract finished
            this.FinishFileExtract(Result);
        } // for (TaskFileIndex)

        this.BroadcastOnFinished(false);
        return null;
    }

    protected void onCancelled() {
        super.onCancelled();

        this.FinishFileExtract(TaskFileStatus.CANCELED);
        this.Cleanup(true);

        this.BroadcastOnFinished(true);

        // Container event
        if (this.AfterCancelled != null) this.AfterCancelled.run();
    }
    public void Cancel(Runnable AfterCancelled) {
        this.AfterCancelled = AfterCancelled;
        this.cancel(true);
    }

    public DefaultTask GetTask() {
        return null;
    }

    @Override
    protected void Cleanup(boolean IsCancelled) {
        // Default implementation of clean-up. Very similar to DownloadAsyncTask implementation
        if (IsCancelled && this.Task.Flag_RemoveAllFilesWhenCancelled) {
            for (ExtractTaskFile file : this.Task.Files) {
                file.OutputResult = TaskFileStatus.CANCELED;
                Utils.RemoveFile(file.OutputDirectory);
            }
        }

        if (this.Task.Flag_ResetTaskAfterFinished || (IsCancelled && this.Task.Flag_ResetTaskWhenCancelled)){
            this.Task.Reset(); // Reset task
        }
    }

    // Utils
    private void FinishFileExtract(TaskFileStatus Status){
        if (Status != TaskFileStatus.SUCCESSFUL) {
            // If flag set - remove failed file from storage
            if (this.Task.Flag_RemoveFailedFile) {
                Utils.RemoveFile(this.Task.Files.get(this.Task.FileIndex).OutputDirectory);
            }
        }

        // Set current file status
        ExtractTaskFile Current = this.Task.Files.get(this.Task.FileIndex);
        Current.OutputResult = Status;
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFileExtractFinished(Current, Status));
    }

    private void BroadcastProgressChanged(){
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnProgressChanged(Task.Status));
    }
    private void BroadcastOnFinished(boolean IsCancelled){
        new Handler(Looper.getMainLooper()).post(() -> this.Task.Callback.OnFinished(IsCancelled));
    }
}