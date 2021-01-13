package com.example.samp_launcher.core.SAMP.Components.ArchiveSystem;

import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFile;
import com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile.ExtractTaskFileInit;
import com.example.samp_launcher.core.SAMP.Components.DefaultTask;
import com.example.samp_launcher.core.SAMP.Components.TaskStatus;

import java.util.ArrayList;

public class ExtractTask extends DefaultTask {
    public ArrayList<ExtractTaskFile> Files;
    public ExtractTaskCallback Callback;

    ExtractTask(int FileIndex, ArrayList<ExtractTaskFileInit> FilepathList, TaskStatus Status, ExtractTaskCallback Callback){
        this.FileIndex = FileIndex;
        this.Status = Status;

        // Set files
        this.SetFilesFromInit(FilepathList);

        // Set callback owner
        this.SetCallback(Callback);
    }

    public void SetFilesFromInit(ArrayList<ExtractTaskFileInit> List){
        this.Files = new ArrayList<>();
        for (ExtractTaskFileInit Init : List) {
            this.Files.add(new ExtractTaskFile(Init));
        }

        this.Status.FilesNumber = this.Files.size();
    }

    public void SetCallback(ExtractTaskCallback Callback){
        if (Callback != this.Callback) {
            this.Callback.Owner.Task = null; // Remove owner from old callback
            this.Callback = Callback;
            this.Callback.Owner.Task = this; // Set this as a owner in new callback
        }
    }

    public void Reset(){
        // Reset
        this.FileIndex = 0;

        for (int c = 0; c < this.Files.size(); ++c){
            this.Files.set(c, new ExtractTaskFile(this.Files.get(c).Filepath, this.Files.get(c).OutputDirectory,
                                                  this.Files.get(c).CreateContainingDirectory));
        }
    }
}