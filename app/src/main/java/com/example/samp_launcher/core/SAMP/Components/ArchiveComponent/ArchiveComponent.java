package com.example.samp_launcher.core.SAMP.Components.ArchiveComponent;

import java.io.File;

public class ArchiveComponent {
    static boolean UnZip(File ZipArchive, File OutputDirectory){
        if (!ZipArchive.exists() || !ZipArchive.isFile()) return false;
        if (OutputDirectory.isFile()) return false;

        // Create dirs ( if they don't exist )
        if (!OutputDirectory.exists()){
            if (!OutputDirectory.mkdirs()) {
                return false;
            }
        }

        // Un-pack


        return true;
    }
}
