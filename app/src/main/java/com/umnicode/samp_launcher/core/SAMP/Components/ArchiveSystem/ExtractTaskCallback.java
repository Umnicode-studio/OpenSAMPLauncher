package com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem;

import com.umnicode.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskFileStatus;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;

class ExtractTaskCallbackOwner{
    public ExtractTask Task;
};

public interface ExtractTaskCallback
{
    ExtractTaskCallbackOwner Owner = new ExtractTaskCallbackOwner();

    void OnStarted();
    default void OnChecksFinished() {};

    void OnFinished(boolean IsCanceled);

    void OnFileExtractStarted(ExtractTaskFile File);
    void OnFileExtractFinished(ExtractTaskFile File, TaskFileStatus Status);

    void OnProgressChanged(TaskStatus Status);

    // Get task method
    default ExtractTask Task(){
        return this.Owner.Task;
    }
}
