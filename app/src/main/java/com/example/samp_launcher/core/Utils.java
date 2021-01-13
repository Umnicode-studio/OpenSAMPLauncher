package com.example.samp_launcher.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
    static public float BytesToMB(float Bytes){
        return Bytes / 1048576; // (1024 * 1024)
    }

    static public ArrayList<String> GetFileExtensions(File file){
        ArrayList<String> List;
        String FileName = file.getName().toLowerCase();

        // Split by dot
        List = new ArrayList<>(Arrays.asList(FileName.split("\\.", -1)));
        if (!List.isEmpty()) List.remove(0);

        return List;
    }

    static public String GetFileLastExtension(File file){
        ArrayList<String> Result = GetFileExtensions(file);
        return Result.isEmpty() ? "" : Result.get(Result.size() - 1);
    }

    static public String GetFileNameWithoutExtension(File file, boolean ConvertToLowercase){
        if (ConvertToLowercase) return file.getName().toLowerCase().split("\\.")[0];
        return file.getName().split("\\.")[0];
    }

    static public boolean RemoveFile(File file){
        if (file != null) return file.delete();
        return false;
    }
}
