package com.example.samp_launcher.core.SAMP.Components.ArchiveSystem.ExtractTaskFile;
import java.io.File;

public class ExtractTaskFileInit{
    public File Filepath;
    public File OutDirectory;
    public boolean CreateContainingFolder;

    public ExtractTaskFileInit(File Filepath, File OutDirectory, boolean CreateContainingFolder){
        this.Filepath = Filepath;
        this.OutDirectory = OutDirectory;
        this.CreateContainingFolder = CreateContainingFolder;
    }
}
