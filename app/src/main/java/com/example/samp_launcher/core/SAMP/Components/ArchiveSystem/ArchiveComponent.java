package com.example.samp_launcher.core.SAMP.Components.ArchiveSystem;

import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit;
import com.example.samp_launcher.core.SAMP.Components.AsyncTaskContainer;
import com.example.samp_launcher.core.SAMP.Components.TaskStatus;
import com.example.samp_launcher.core.Utils;

import java.io.File;
import java.util.ArrayList;

public class ArchiveComponent {
    public static ExtractTask CreateTask(ArrayList<ExtractTaskFileInit> FilepathList, ExtractTaskCallback Callback){
        // Create default task
        return new ExtractTask(0, FilepathList, TaskStatus.CreateEmpty(FilepathList.size()), Callback);
    }

    static public AsyncTaskContainer RunTask(ExtractTask Task){
        return new AsyncTaskContainer(new ExtractAsyncTask(Task));
    }

    static public ArchiveType GetTypeOfArchive(File Archive){
        String Extension = Utils.GetFileLastExtension(Archive);
        if (Extension.equals("zip")){
            return ArchiveType.ZIP;
        } else{
            return ArchiveType.UNSUPPORTED;
        }
    }
}
