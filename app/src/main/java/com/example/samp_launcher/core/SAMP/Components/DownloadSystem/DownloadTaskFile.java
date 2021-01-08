package com.example.samp_launcher.core.SAMP.Components.DownloadSystem;

import java.io.File;
import java.net.URL;

public class DownloadTaskFile {
    public File OutputFilename = null;
    public boolean OutputResult = false;

    public URL url;

    DownloadTaskFile(URL url){
        this.url = url;
    }
}
