package com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile;

import com.example.samp_launcher.core.SAMP.Components.TaskFileStatus;

import java.io.File;

public class ExtractTaskFile {
    public TaskFileStatus OutputResult = TaskFileStatus.NONE;

    public File OutputDirectory = null;
    public File Filepath;
    public boolean CreateContainingDirectory;

    public ExtractTaskFile(File Filepath, File OutputDirectory, boolean CreateContainingDirectory) {
        this.Filepath = Filepath;
        this.OutputDirectory = OutputDirectory;
        this.CreateContainingDirectory = CreateContainingDirectory;
    }

    public ExtractTaskFile(ExtractTaskFileInit Init){
        this.Filepath = Init.Filepath;
        this.OutputDirectory = Init.OutDirectory;
        this.CreateContainingDirectory = Init.CreateContainingFolder;
    }
}
