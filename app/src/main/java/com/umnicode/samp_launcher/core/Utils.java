package com.umnicode.samp_launcher.core;

import android.util.Log;

import com.umnicode.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    public static <ObjType> ObjType DeepCloneObject(ObjType Object){
        try{
            Object Clone = Object.getClass().newInstance();

            for (Field field : Object.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                // Skip if filed is null or final
                if(field.get(Object) == null || Modifier.isFinal(field.getModifiers())){
                    continue;
                }

                if(field.getType().isPrimitive() || field.getType().equals(String.class)
                        || field.getType().getSuperclass().equals(Number.class)
                        || field.getType().equals(Boolean.class)){
                    field.set(Clone, field.get(Object));
                }else{
                    Object childObj = field.get(Object);
                    if(childObj == Object){ // Self-reference check
                        field.set(Clone, Clone);
                    }else{
                        field.set(Clone, DeepCloneObject(field.get(Object)));
                    }
                }
            }

            return (ObjType)Clone;
        }catch(Exception e){
            Log.e("DeepClone", "Failed to clone object - " + Object.getClass().getName());
            return null;
        }
    }
}
